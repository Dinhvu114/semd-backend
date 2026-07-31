package com.semd.backend.dto.emergencyCall;

public record AICallbackRequest(
        Integer call_id,
        String transcript,
        String urgency,
        Double confidence,
        java.util.List<String> symptoms
    ) {}
