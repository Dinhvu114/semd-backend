package com.semd.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DispatchResourceDto(
    Integer id,
    String resourceCode,
    Integer resourceTypeId,
    String resourceTypeName,
    Integer zoneId,
    String zoneName,
    Integer providerId,
    String providerName,
    Integer currentDriverId,
    String currentDriverName,
    String status,
    Double longitude,
    Double latitude,
    Map<String, Object> extendedAttributes,
    LocalDateTime updatedAt
) {}
