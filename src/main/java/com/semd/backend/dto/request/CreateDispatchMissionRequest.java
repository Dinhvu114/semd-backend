package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public class CreateDispatchMissionRequest {

    @NotNull(message = "request_id không được để trống")
    private Integer requestId;

    @NotNull(message = "resource_id không được để trống")
    private Integer resourceId;

    private String destinationName;
    private String notes;

    // Getters & Setters
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}