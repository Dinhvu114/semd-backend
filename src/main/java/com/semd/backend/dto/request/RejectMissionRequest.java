package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RejectMissionRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}