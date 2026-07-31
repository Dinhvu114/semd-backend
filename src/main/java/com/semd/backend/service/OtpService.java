package com.semd.backend.service;

import com.semd.backend.dto.OtpVerificationResponse;
import com.semd.backend.entity.IntegrationLog;
import com.semd.backend.entity.IntegrationPartner;
import com.semd.backend.exception.OtpDeliveryException;
import com.semd.backend.repository.IntegrationLogRepository;
import com.semd.backend.repository.IntegrationPartnerRepository;
import com.semd.backend.service.otp.OtpDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class OtpService {

    private static final String OTP_KEY_PREFIX = "otp:code:";
    private static final String COOLDOWN_KEY_PREFIX = "otp:cooldown:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp:attempts:";
    private static final String REGISTRATION_TOKEN_KEY_PREFIX = "otp:registration-token:";
    private static final DefaultRedisScript<Long> CONSUME_REGISTRATION_TOKEN_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] " +
                            "then return redis.call('DEL', KEYS[1]) else return 0 end",
                    Long.class
            );

    private final IntegrationPartnerRepository partnerRepo;
    private final IntegrationLogRepository logRepo;
    private final StringRedisTemplate redisTemplate;
    private final OtpDeliveryService deliveryService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration otpTtl;
    private final Duration resendCooldown;
    private final Duration registrationTokenTtl;
    private final int maxAttempts;
    private final String defaultCountryCode;
    private final String hashSecret;

    public OtpService(
            IntegrationPartnerRepository partnerRepo,
            IntegrationLogRepository logRepo,
            StringRedisTemplate redisTemplate,
            OtpDeliveryService deliveryService,
            @Value("${app.otp.ttl-seconds:300}") long otpTtlSeconds,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${app.otp.registration-token-ttl-seconds:600}") long registrationTokenTtlSeconds,
            @Value("${app.otp.max-verification-attempts:5}") int maxAttempts,
            @Value("${app.otp.default-country-code:+84}") String defaultCountryCode,
            @Value("${app.otp.hash-secret:${jwt.secret}}") String hashSecret
    ) {
        this.partnerRepo = partnerRepo;
        this.logRepo = logRepo;
        this.redisTemplate = redisTemplate;
        this.deliveryService = deliveryService;
        this.otpTtl = Duration.ofSeconds(otpTtlSeconds);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.registrationTokenTtl = Duration.ofSeconds(registrationTokenTtlSeconds);
        this.maxAttempts = maxAttempts;
        this.defaultCountryCode = defaultCountryCode;
        this.hashSecret = hashSecret;
    }

    public String generateAndSendOtp(String rawPhoneNumber) {
        String phoneNumber = normalizePhoneNumber(rawPhoneNumber);
        String cooldownKey = COOLDOWN_KEY_PREFIX + phoneNumber;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", resendCooldown);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalArgumentException("Vui lòng chờ trước khi yêu cầu gửi lại mã OTP");
        }

        String otpCode = "%06d".formatted(secureRandom.nextInt(1_000_000));
        String otpKey = OTP_KEY_PREFIX + phoneNumber;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + phoneNumber;

        redisTemplate.opsForValue().set(otpKey, hashOtp(phoneNumber, otpCode), otpTtl);
        redisTemplate.opsForValue().set(attemptsKey, String.valueOf(maxAttempts), otpTtl);

        try {
            deliveryService.send(phoneNumber, otpCode);
            writeSendLog(phoneNumber, true, null);
        } catch (RuntimeException exception) {
            redisTemplate.delete(List.of(otpKey, attemptsKey, cooldownKey));
            writeSendLog(phoneNumber, false, exception.getMessage());
            if (exception instanceof OtpDeliveryException otpDeliveryException) {
                throw otpDeliveryException;
            }
            throw new OtpDeliveryException("Không thể gửi mã OTP", exception);
        }

        return deliveryService.exposesCodeToClient() ? otpCode : null;
    }

    @Transactional
    public boolean verifyOtp(String rawPhoneNumber, String code) {
        String phoneNumber = normalizePhoneNumber(rawPhoneNumber);
        String otpKey = OTP_KEY_PREFIX + phoneNumber;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + phoneNumber;
        String storedHash = redisTemplate.opsForValue().get(otpKey);

        if (storedHash == null || code == null) {
            logVerificationResult(phoneNumber, false, "OTP_NOT_FOUND_OR_EXPIRED");
            return false;
        }

        String remainingValue = redisTemplate.opsForValue().get(attemptsKey);
        int remainingAttempts = remainingValue == null ? 0 : Integer.parseInt(remainingValue);
        if (remainingAttempts <= 0) {
            redisTemplate.delete(List.of(otpKey, attemptsKey));
            logVerificationResult(phoneNumber, false, "TOO_MANY_ATTEMPTS");
            return false;
        }

        boolean matches = MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                hashOtp(phoneNumber, code).getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            Long remaining = redisTemplate.opsForValue().decrement(attemptsKey);
            if (remaining != null && remaining <= 0) {
                redisTemplate.delete(List.of(otpKey, attemptsKey));
            }
            logVerificationResult(phoneNumber, false, "OTP_MISMATCH");
            return false;
        }

        redisTemplate.delete(List.of(otpKey, attemptsKey, COOLDOWN_KEY_PREFIX + phoneNumber));
        logVerificationResult(phoneNumber, true, "VERIFIED");
        return true;
    }

    public OtpVerificationResponse verifyAndIssueRegistrationToken(String phoneNumber, String otpCode) {
        if (!verifyOtp(phoneNumber, otpCode)) {
            throw new IllegalArgumentException("Mã xác thực OTP không chính xác hoặc đã hết hạn");
        }

        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String verificationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        redisTemplate.opsForValue().set(
                registrationTokenKey(verificationToken),
                normalizedPhoneNumber,
                registrationTokenTtl
        );

        return new OtpVerificationResponse(
                verificationToken,
                registrationTokenTtl.toSeconds()
        );
    }

    public boolean consumeRegistrationToken(String phoneNumber, String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            return false;
        }

        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        Long consumed = redisTemplate.execute(
                CONSUME_REGISTRATION_TOKEN_SCRIPT,
                List.of(registrationTokenKey(verificationToken)),
                normalizedPhoneNumber
        );
        return Long.valueOf(1L).equals(consumed);
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }

        String normalized = rawPhoneNumber.trim().replaceAll("[\\s().-]", "");
        if (normalized.startsWith("0")) {
            normalized = defaultCountryCode + normalized.substring(1);
        } else if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }

        if (!normalized.matches("\\+[1-9]\\d{7,14}")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng quốc tế E.164");
        }
        return normalized;
    }

    private String hashOtp(String phoneNumber, String otpCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((hashSecret + ":" + phoneNumber + ":" + otpCode)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể bảo vệ mã OTP", exception);
        }
    }

    private String registrationTokenKey(String verificationToken) {
        return REGISTRATION_TOKEN_KEY_PREFIX + hashValue(verificationToken);
    }

    private String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (hashSecret + ":" + value).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể bảo vệ token xác minh", exception);
        }
    }

    private void writeSendLog(String phoneNumber, boolean success, String reason) {
        IntegrationLog log = new IntegrationLog();
        log.setPartner(findActiveTelco());
        log.setDirection("OUTBOUND");
        log.setEventType("SEND_OTP");
        log.setPayload(Map.of(
                "phoneNumber", maskPhoneNumber(phoneNumber),
                "providerUsed", deliveryService.providerName(),
                "status", success ? "SUCCESS" : "FAILED",
                "reason", reason == null ? "" : reason
        ));
        log.setStatusCode(success ? 200 : 503);
        logRepo.save(log);
    }

    private void logVerificationResult(String phoneNumber, boolean success, String reason) {
        IntegrationLog log = new IntegrationLog();
        log.setPartner(findActiveTelco());
        log.setDirection("INBOUND");
        log.setEventType("VERIFY_OTP");
        log.setPayload(Map.of(
                "phoneNumber", maskPhoneNumber(phoneNumber),
                "status", success ? "SUCCESS" : "FAILED",
                "reason", reason
        ));
        log.setStatusCode(success ? 200 : 400);
        logRepo.save(log);
    }

    private IntegrationPartner findActiveTelco() {
        List<IntegrationPartner> telcos = partnerRepo.findByPartnerTypeAndIsActiveTrue("TELCO");
        return telcos.isEmpty() ? null : telcos.get(0);
    }

    private String maskPhoneNumber(String phoneNumber) {
        return phoneNumber.length() <= 4
                ? "***"
                : "***" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
