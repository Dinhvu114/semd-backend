package com.semd.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CoordinateDto(
    @NotNull(message = "Kinh độ (longitude) không được để trống")
    Double longitude,

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    Double latitude
) {}
