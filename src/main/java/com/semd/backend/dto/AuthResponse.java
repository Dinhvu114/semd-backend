package com.semd.backend.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    Integer userId,
    String username,
    String fullName,
    java.util.Set<String> roles
) {}
