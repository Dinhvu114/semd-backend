package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SeverityUpdateRequest(
        @NotBlank(message = "Mức độ nghiêm trọng không được để trống")
        @Pattern(
                regexp = "(?i)LOW|MEDIUM|HIGH|CRITICAL",
                message = "Mức độ nghiêm trọng phải là LOW, MEDIUM, HIGH hoặc CRITICAL"
        )
        String severity
) {}
