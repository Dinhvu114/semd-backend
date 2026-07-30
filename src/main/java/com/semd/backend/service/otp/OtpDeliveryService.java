package com.semd.backend.service.otp;

public interface OtpDeliveryService {
    void send(String phoneNumber, String otpCode);

    default boolean exposesCodeToClient() {
        return false;
    }

    default String providerName() {
        return getClass().getSimpleName();
    }
}
