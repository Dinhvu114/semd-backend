package com.semd.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DispatchResourceResponse(
        Integer id,
        String resourceCode,
        String resourceType,
        String status,
        Integer driverId,
        String driverName,
        Double latitude,
        Double longitude,
        Map<String, Object> extendedAttributes,
        LocalDateTime updatedAt
) {
}