package com.semd.backend.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.otp.delivery-mode", havingValue = "mock", matchIfMissing = true)
public class MockOtpDeliveryService implements OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(MockOtpDeliveryService.class);

    @Override
    public void send(String phoneNumber, String otpCode) {
        log.info("Mock OTP generated for {}", maskPhoneNumber(phoneNumber));
    }

    @Override
    public boolean exposesCodeToClient() {
        return true;
    }

    @Override
    public String providerName() {
        return "MOCK";
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return "***" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
