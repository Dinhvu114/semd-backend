package com.semd.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DispatchRequestDetailDto(
        Integer id,
        // Emergency Call info
        Integer callId,
        String reporterPhone,
        String reporterName,
        String audioUrl,
        String description,
        // AI Result
        String aiTranscript,
        String aiUrgencyPrediction,
        BigDecimal aiConfidenceScore,
        // Dispatch info
        String urgencyLevel,
        String triageLevel,
        String reviewNote,
        String status,
        Double longitude,
        Double latitude,
        // Zone & Service
        String zoneName,
        String serviceTypeName,
        // Dispatcher
        String confirmedByName,
        LocalDateTime confirmedAt,
        String verifiedByName,
        LocalDateTime verifiedAt,
        String verificationNote,
        String rejectedByName,
        LocalDateTime rejectedAt,
        String rejectionReason,
        String confirmedAddress,
        BigDecimal confirmedLatitude,
        BigDecimal confirmedLongitude,
        String confirmedUrgencyLevel,
        // Missions
        long missionCount,
        // Extended
        Map<String, Object> extendedRequirements,
        LocalDateTime createdAt
) {}
