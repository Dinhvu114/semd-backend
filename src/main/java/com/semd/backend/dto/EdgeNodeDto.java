package com.semd.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EdgeNodeDto(
    Integer id,
    String nodeName,
    List<CoordinateDto> coverageArea,
    Boolean isActive,
    LocalDateTime createdAt
) {}
