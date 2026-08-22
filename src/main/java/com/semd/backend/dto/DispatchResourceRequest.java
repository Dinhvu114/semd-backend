package com.semd.backend.dto;

import com.semd.backend.entity.DispatchResourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record DispatchResourceRequest(
        @NotBlank(message = "Mã xe cứu thương không được để trống")
        @Size(max = 50, message = "Mã xe cứu thương không vượt quá 50 ký tự")
        String resourceCode,

        Integer resourceTypeId,
        Integer providerId,
        Integer currentDriverId,

        @NotNull(message = "Trạng thái không được để trống")
        DispatchResourceStatus status,

        Double longitude,
        Double latitude,
        Map<String, Object> extendedAttributes
) {}