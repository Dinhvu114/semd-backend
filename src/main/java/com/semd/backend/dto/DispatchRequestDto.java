package com.semd.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DispatchRequestDto(
    Integer id,
    Integer callId,
    String reporterPhone,
    Integer serviceTypeId,
    String serviceTypeName,
    Integer zoneId,
    String zoneName,
    Integer createdByDispatcherId,
    String createdByDispatcherName,
    String urgencyLevel,
    Double longitude,
    Double latitude,
    String status,
    Map<String, Object> extendedRequirements,
    LocalDateTime createdAt
) {}
