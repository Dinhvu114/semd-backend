package com.semd.backend.dto.emergencyCall;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SosRequest(
    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ (latitude) phải lớn hơn hoặc bằng -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ (latitude) phải nhỏ hơn hoặc bằng 90")
    Double latitude,

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ (longitude) phải lớn hơn hoặc bằng -180")
    @DecimalMax(value = "180.0", message = "Kinh độ (longitude) phải nhỏ hơn hoặc bằng 180")
    Double longitude
) {}
