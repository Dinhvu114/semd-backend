package com.semd.backend.dto;

public record OtpVerificationResponse(
        String verificationToken,
        long expiresInSeconds
) {}
