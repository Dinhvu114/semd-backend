package com.semd.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OperationZoneDto(
    Integer id,
    String zoneName,
    List<CoordinateDto> coverageArea,
    Boolean isActive,
    LocalDateTime createdAt
) {}
