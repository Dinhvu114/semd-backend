package com.semd.backend.dto.emergencyCall;

import java.time.LocalDateTime;

public record CallStatusResponse(
        Integer callId,
        String callStatus,
        Integer dispatchRequestId,
        String dispatchRequestStatus,
        Integer missionId,
        String missionStatus,
        LocalDateTime updatedAt
) {}
