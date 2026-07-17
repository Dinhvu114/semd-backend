package com.semd.backend.dto;

import com.semd.backend.entity.DispatchResourceStatus;
import jakarta.validation.constraints.NotNull;

public record ResourceStatusUpdateRequest(
    @NotNull(message = "Trạng thái không được để trống")
    DispatchResourceStatus status
) {}
