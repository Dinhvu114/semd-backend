package com.semd.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProviderDto(
    Integer id,
    Integer ownerUserId,
    String ownerUsername,
    String ownerFullName,
    String providerName,
    String providerType,
    String businessLicense,
    String contactPhone,
    String contactAddress,
    BigDecimal commissionRate,
    Boolean isVerified,
    Boolean isActive,
    LocalDateTime createdAt
) {}
