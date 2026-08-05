package com.semd.backend.dto;

import java.time.LocalDateTime;

public record UserDto(
    Integer id,
    String username,
    String fullName,
    String phoneNumber,
    String email,
    java.util.Set<String> roles,
    Integer providerId,
    Boolean isActive,
    LocalDateTime createdAt
) {}
