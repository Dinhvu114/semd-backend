package com.semd.backend.dto.emergencyCall;

    public record VoiceCallRequest(
        String audioObjectKey,
        LocationDto location
    ) {}
