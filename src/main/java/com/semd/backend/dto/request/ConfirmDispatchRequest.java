package com.semd.backend.dto.request;

import jakarta.validation.constraints.Size;

public record ConfirmDispatchRequest(
        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note
) {}
