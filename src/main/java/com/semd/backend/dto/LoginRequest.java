package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Schema(example = "huy", description = "Tên đăng nhập của tài khoản")
    String username,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(example = "123456", description = "Mật khẩu của tài khoản")
    String password
) {}
