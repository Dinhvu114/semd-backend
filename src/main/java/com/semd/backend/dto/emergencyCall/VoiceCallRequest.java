package com.semd.backend.dto.emergencyCall;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoiceCallRequest(
    @NotBlank(message = "Khóa tệp ghi âm (audioObjectKey) không được để trống")
    String audioObjectKey,

    @NotNull(message = "Vị trí (location) không được để trống")
    @Valid
    LocationDto location
) {}
