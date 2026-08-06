package com.semd.backend.dto.emergencyCall;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmergencyCallResponse(
        Integer callId,
        Integer dispatchRequestId,
        String callType,
        String callStatus,
        String dispatchRequestStatus,
        String description,
        String reporterName,
        String reporterPhone,
        String audioUrl,
        String aiTranscript,
        String aiUrgencyPrediction,
        BigDecimal aiConfidenceScore,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {}
