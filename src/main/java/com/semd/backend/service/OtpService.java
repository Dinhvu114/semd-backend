package com.semd.backend.service;

import com.semd.backend.entity.IntegrationLog;
import com.semd.backend.entity.IntegrationPartner;
import com.semd.backend.repository.IntegrationLogRepository;
import com.semd.backend.repository.IntegrationPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final IntegrationPartnerRepository partnerRepo;
    private final IntegrationLogRepository logRepo;

    // Cache lưu trữ mã OTP trong bộ nhớ tạm thời
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public OtpService(IntegrationPartnerRepository partnerRepo, IntegrationLogRepository logRepo) {
        this.partnerRepo = partnerRepo;
        this.logRepo = logRepo;
    }

    private record OtpData(String code, LocalDateTime expiresAt) {
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * Sinh và "gửi" mã OTP đến số điện thoại (mô phỏng).
     *
     * @param phoneNumber Số điện thoại nhận OTP
     * @return Mã OTP đã được sinh ra (thuận tiện cho việc test/mock)
     */
    @Transactional
    public String generateAndSendOtp(String phoneNumber) {
        // Sinh ngẫu nhiên mã 6 chữ số
        String otpCode = String.valueOf(100000 + random.nextInt(900000));
        
        // OTP có hiệu lực trong 5 phút
        otpCache.put(phoneNumber, new OtpData(otpCode, LocalDateTime.now().plusMinutes(5)));

        // Tìm đối tác viễn thông hoạt động
        List<IntegrationPartner> telcos = partnerRepo.findByPartnerTypeAndIsActiveTrue("TELCO");
        IntegrationPartner activeTelco = telcos.isEmpty() ? null : telcos.get(0);

        // Ghi nhật ký cuộc gọi tích hợp (Outbound)
        IntegrationLog outboundLog = new IntegrationLog();
        outboundLog.setPartner(activeTelco);
        outboundLog.setDirection("OUTBOUND");
        outboundLog.setEventType("SEND_OTP");
        outboundLog.setPayload(Map.of(
                "phoneNumber", phoneNumber,
                "otpCode", otpCode,
                "message", "Mã xác thực OTP gửi từ đối tác viễn thông",
                "providerUsed", activeTelco != null ? activeTelco.getPartnerName() : "MOCK_FALLBACK_PROVIDER"
        ));
        outboundLog.setStatusCode(200);
        logRepo.save(outboundLog);

        return otpCode;
    }

    /**
     * Xác thực mã OTP.
     *
     * @param phoneNumber Số điện thoại cần xác thực
     * @param code Mã OTP do người dùng nhập
     * @return true nếu mã chính xác và còn hạn, ngược lại false
     */
    @Transactional
    public boolean verifyOtp(String phoneNumber, String code) {
        // Cho phép mã cố định "123456" cho tất cả các mục đích chạy mock/testing
        if ("123456".equals(code)) {
            logVerificationResult(phoneNumber, code, true);
            return true;
        }

        OtpData otpData = otpCache.get(phoneNumber);
        if (otpData == null || otpData.isExpired() || !otpData.code().equals(code)) {
            logVerificationResult(phoneNumber, code, false);
            return false;
        }

        // Xác thực thành công -> xóa mã
        otpCache.remove(phoneNumber);
        logVerificationResult(phoneNumber, code, true);
        return true;
    }

    private void logVerificationResult(String phoneNumber, String code, boolean isSuccess) {
        List<IntegrationPartner> telcos = partnerRepo.findByPartnerTypeAndIsActiveTrue("TELCO");
        IntegrationPartner activeTelco = telcos.isEmpty() ? null : telcos.get(0);

        // Ghi nhật ký tiếp nhận xác thực (Inbound)
        IntegrationLog inboundLog = new IntegrationLog();
        inboundLog.setPartner(activeTelco);
        inboundLog.setDirection("INBOUND");
        inboundLog.setEventType("VERIFY_OTP");
        inboundLog.setPayload(Map.of(
                "phoneNumber", phoneNumber,
                "otpCodeSent", code,
                "status", isSuccess ? "SUCCESS" : "FAILED",
                "reason", isSuccess ? "Mã khớp" : "Mã không hợp lệ hoặc đã hết hạn"
        ));
        inboundLog.setStatusCode(isSuccess ? 200 : 400);
        logRepo.save(inboundLog);
    }
}
