package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "Tài khoản (Email hoặc Số điện thoại) không được để trống")
    @Schema(example = "reporter01@gmail.com", description = "Email hoặc Số điện thoại để khôi phục mật khẩu")
    String identity
) {}
