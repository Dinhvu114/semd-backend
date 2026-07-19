package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Tài khoản (Email hoặc Số điện thoại) không được để trống")
    @Schema(example = "reporter01@gmail.com", description = "Email hoặc Số điện thoại")
    String identity,

    @NotBlank(message = "Mã xác thực OTP không được để trống")
    @Schema(example = "123456", description = "Mã xác thực OTP đã nhận")
    String otpCode,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 100, message = "Mật khẩu mới không vượt quá 100 ký tự")
    @Schema(example = "654321", description = "Mật khẩu mới của tài khoản")
    String newPassword
) {}
