package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CancelMissionRequest {

    @NotBlank(message = "Lý do huỷ không được để trống")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}