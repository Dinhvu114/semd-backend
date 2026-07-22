package com.semd.backend.dto.response;

import java.util.List;

public record RecommendationItemDto(
        Integer resourceId,
        String resourceCode,
        double score,
        long etaSeconds,
        double distanceKm,
        List<String> reason
) {}
