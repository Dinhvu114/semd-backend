package com.semd.backend.dto;

import java.util.Map;

public record MedicalHospitalDto(
    Integer id,
    Integer ownerUserId,
    String hospitalName,
    String hospitalAddress,
    Double longitude,
    Double latitude,
    Map<String, Object> capabilities,
    String contactPhone,
    Boolean isActive
) {}
