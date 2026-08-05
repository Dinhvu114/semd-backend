package com.semd.backend.dto.response;

public record StatisticsResponse(
        long pending,
        long aiAnalyzed,
        long confirmed,
        long dispatching,
        long completedToday,
        long rejected
        // long cancelled
) {}
