package com.semd.backend.dto.emergencyCall;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VoiceCallRequest(
    @NotBlank(message = "Khóa tệp ghi âm (audioObjectKey) không được để trống")
    String audioObjectKey,

    @NotNull(message = "Vị trí (location) không được để trống")
    @Valid
    LocationDto location,

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    String description
) {}
