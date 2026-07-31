package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
        String phoneNumber,

        @NotBlank(message = "Mã xác thực OTP không được để trống")
        @Pattern(regexp = "\\d{6}", message = "Mã OTP phải gồm 6 chữ số")
        String otpCode
) {}
