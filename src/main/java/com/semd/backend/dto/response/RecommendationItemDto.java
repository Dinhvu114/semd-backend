package com.semd.backend.dto.response;

import java.util.List;
import java.time.LocalDateTime;

public record RecommendationItemDto(
        Integer resourceId,
        String resourceCode,
        int rank,
        double score,
        long etaSeconds,
        double distanceKm,
        LocalDateTime locationUpdatedAt,
        String etaSource,
        ScoreBreakdown breakdown,
        List<String> reasons,
        List<String> warnings
) {
    public record ScoreBreakdown(
            double eta,
            double distance,
            double capability,
            double freshness,
            double risk
    ) {}
}
