package com.semd.backend.dto.emergencyCall;

import com.semd.backend.dto.response.TrackingResponse;

public record CallTrackingResponse(
        Integer callId,
        String callStatus,
        Integer dispatchRequestId,
        String dispatchRequestStatus,
        Integer missionId,
        String missionStatus,
        Integer resourceId,
        String resourceCode,
        String resourceStatus,
        Double resourceLongitude,
        Double resourceLatitude,
        TrackingResponse tracking
) {}
