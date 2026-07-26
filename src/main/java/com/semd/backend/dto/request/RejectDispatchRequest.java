package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDispatchRequest(
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
        String reason
) {}
