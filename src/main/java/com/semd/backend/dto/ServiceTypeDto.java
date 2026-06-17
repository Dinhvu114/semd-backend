package com.semd.backend.dto;

public record ServiceTypeDto(
    Integer id,
    String typeCode,
    String displayName,
    Integer priorityWeight
) {}
