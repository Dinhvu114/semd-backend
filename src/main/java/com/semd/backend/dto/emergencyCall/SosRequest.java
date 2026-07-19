package com.semd.backend.dto.emergencyCall;

    public record SosRequest(
        Double latitude,
        Double longitude
    ) {}