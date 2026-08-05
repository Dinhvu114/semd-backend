package com.semd.backend.service;

import com.semd.backend.repository.IntegrationLogRepository;
import com.semd.backend.repository.IntegrationPartnerRepository;
import com.semd.backend.service.otp.OtpDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    private static final String HASH_SECRET = "test-hash-secret";

    private IntegrationPartnerRepository partnerRepo;
    private IntegrationLogRepository logRepo;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OtpDeliveryService deliveryService;
    private OtpService otpService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        partnerRepo = mock(IntegrationPartnerRepository.class);
        logRepo = mock(IntegrationLogRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        deliveryService = mock(OtpDeliveryService.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(partnerRepo.findByPartnerTypeAndIsActiveTrue("TELCO")).thenReturn(List.of());

        otpService = new OtpService(
                partnerRepo,
                logRepo,
                redisTemplate,
                deliveryService,
                300,
                60,
                600,
                5,
                "+84",
                HASH_SECRET
        );
    }

    @Test
    void generateAndSendOtp_returnsCodeOnlyWhenDeliveryModeExposesIt() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(true);
        when(deliveryService.exposesCodeToClient()).thenReturn(true);
        when(deliveryService.providerName()).thenReturn("MOCK");

        String responseCode = otpService.generateAndSendOtp("0987 654 321");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryService).send(eq("+84987654321"), codeCaptor.capture());
        assertEquals(codeCaptor.getValue(), responseCode);
        assertTrue(responseCode.matches("\\d{6}"));
        verify(valueOperations).set(
                eq("otp:code:+84987654321"),
                argThat(value -> !value.equals(responseCode)),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void verifyOtp_acceptsStoredCodeAndDeletesItAfterUse() {
        String phoneNumber = "+84987654321";
        String code = "748291";
        when(valueOperations.get("otp:code:" + phoneNumber))
                .thenReturn(hash(phoneNumber, code));
        when(valueOperations.get("otp:attempts:" + phoneNumber)).thenReturn("5");

        assertTrue(otpService.verifyOtp("0987654321", code));
        verify(redisTemplate).delete(List.of(
                "otp:code:" + phoneNumber,
                "otp:attempts:" + phoneNumber,
                "otp:cooldown:" + phoneNumber
        ));
    }

    @Test
    void verifyOtp_doesNotAcceptLegacyFixedCodeWithoutStoredOtp() {
        when(valueOperations.get("otp:code:+84987654321")).thenReturn(null);

        assertFalse(otpService.verifyOtp("0987654321", "123456"));
    }

    @Test
    void generateAndSendOtp_rejectsRequestsDuringCooldown() {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> otpService.generateAndSendOtp("0987654321")
        );

        assertEquals("Vui lòng chờ trước khi yêu cầu gửi lại mã OTP", exception.getMessage());
        verifyNoInteractions(deliveryService);
    }

    @Test
    void consumeRegistrationToken_acceptsOnlyThePhoneBoundToTheToken() {
        String token = "registration-verification-token";
        String tokenKey = "otp:registration-token:" + hashToken(token);
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(tokenKey)),
                eq("+84987654321")
        )).thenReturn(1L).thenReturn(0L);

        assertTrue(otpService.consumeRegistrationToken("0987654321", token));
        assertFalse(otpService.consumeRegistrationToken("0987654321", token));
    }

    private String hash(String phoneNumber, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((HASH_SECRET + ":" + phoneNumber + ":" + code)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (HASH_SECRET + ":" + token).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
