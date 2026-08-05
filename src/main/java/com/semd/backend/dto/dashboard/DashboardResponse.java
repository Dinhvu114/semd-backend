package com.semd.backend.dto.dashboard;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        Meta meta,
        ResolvedFilter filters,
        Map<String, Object> kpis,
        List<Map<String, Object>> series,
        Map<String, List<Map<String, Object>>> breakdowns,
        Map<String, List<Map<String, Object>>> details
) {
    public record Meta(Instant generatedAt, String timezone, Map<String, Object> scope) {}
    public record ResolvedFilter(
            LocalDateTime from, LocalDateTime to, String timezone,
            Integer providerId, String granularity) {}
}
