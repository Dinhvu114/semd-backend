package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record DispatchResourceRequest(
    @NotBlank(message = "Mã xe cứu thương không được để trống")
    @Size(max = 50, message = "Mã xe cứu thương không vượt quá 50 ký tự")
    String resourceCode,

    Integer resourceTypeId,
    Integer edgeNodeId,
    Integer providerId,
    Integer currentDriverId,

    @NotBlank(message = "Trạng thái không được để trống")
    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    String status,

    Double longitude,
    Double latitude,
    Map<String, Object> extendedAttributes
) {}
