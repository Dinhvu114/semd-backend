package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendOtpRequest(
    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
    @Schema(example = "0987654321", description = "Số điện thoại nhận mã OTP")
    String phoneNumber
) {}
