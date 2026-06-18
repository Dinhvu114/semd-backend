package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResourceStatusUpdateRequest(
    @NotBlank(message = "Trạng thái không được để trống")
    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    String status
) {}
