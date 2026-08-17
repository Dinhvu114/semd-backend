package com.semd.backend.dto.response;

import java.time.LocalDateTime;

public class ActiveMissionResponse {
    private Integer id;
    private Integer requestId;
    private Integer resourceId;
    private String destinationName;
    private String status;
    private LocalDateTime dispatchedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime arrivedSceneAt;
    private LocalDateTime startTransportAt;
    private LocalDateTime arrivedHospitalAt;
    private String notes;
    // Thêm thông tin hữu ích cho driver
    private String urgencyLevel;
    private Long simulationId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getEnRouteAt() { return enRouteAt; }
    public void setEnRouteAt(LocalDateTime enRouteAt) { this.enRouteAt = enRouteAt; }
    public LocalDateTime getArrivedSceneAt() { return arrivedSceneAt; }
    public void setArrivedSceneAt(LocalDateTime arrivedSceneAt) { this.arrivedSceneAt = arrivedSceneAt; }
    public LocalDateTime getStartTransportAt() { return startTransportAt; }
    public void setStartTransportAt(LocalDateTime startTransportAt) { this.startTransportAt = startTransportAt; }
    public LocalDateTime getArrivedHospitalAt() { return arrivedHospitalAt; }
    public void setArrivedHospitalAt(LocalDateTime arrivedHospitalAt) { this.arrivedHospitalAt = arrivedHospitalAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
}