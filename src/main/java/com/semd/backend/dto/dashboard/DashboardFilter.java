package com.semd.backend.dto.dashboard;

import java.time.LocalDateTime;

public record DashboardFilter(
        LocalDateTime from,
        LocalDateTime to,
        String timezone,
        Integer providerId,
        String granularity
) {}
