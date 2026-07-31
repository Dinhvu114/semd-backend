package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token không được để trống")
    String refreshToken
) {}
