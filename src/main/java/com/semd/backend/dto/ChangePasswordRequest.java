package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    @Schema(example = "123456", description = "Mật khẩu hiện tại")
    String oldPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 100, message = "Mật khẩu mới không vượt quá 100 ký tự")
    @Schema(example = "654321", description = "Mật khẩu mới")
    String newPassword
) {}
