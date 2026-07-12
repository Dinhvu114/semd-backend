package com.semd.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record DispatchRequestDto(
    Integer id,
    Integer callId,
    Integer serviceTypeId,
    String serviceTypeName,
    Integer edgeNodeId,
    String edgeNodeName,
    Integer createdByDispatcherId,
    String createdByDispatcherName,
    String urgencyLevel,
    Double longitude,
    Double latitude,
    String status,
    Map<String, Object> extendedRequirements,
    Boolean isSyncedToCloud,
    LocalDateTime createdAt
) {}
