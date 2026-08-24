package com.semd.backend.dto.response;

import java.time.LocalDateTime;

public class DispatchMissionResponse {
    private Integer id;
    private Integer requestId;
    private Integer resourceId;
    private String destinationName;
    private String status;
    private LocalDateTime dispatchedAt;
    private String notes;
    private String rejectReason;
    private LocalDateTime enRouteAt;
    private LocalDateTime arrivedSceneAt;
    private LocalDateTime startTransportAt;
    private LocalDateTime arrivedHospitalAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private Integer destinationId;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationAddress;

    // Getters & Setters
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
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getEnRouteAt() { return enRouteAt; }
    public void setEnRouteAt(LocalDateTime enRouteAt) { this.enRouteAt = enRouteAt; }
    public LocalDateTime getArrivedSceneAt() { return arrivedSceneAt; }
    public void setArrivedSceneAt(LocalDateTime arrivedSceneAt) { this.arrivedSceneAt = arrivedSceneAt; }
    public LocalDateTime getStartTransportAt() { return startTransportAt; }
    public void setStartTransportAt(LocalDateTime startTransportAt) { this.startTransportAt = startTransportAt; }
    public LocalDateTime getArrivedHospitalAt() { return arrivedHospitalAt; }
    public void setArrivedHospitalAt(LocalDateTime arrivedHospitalAt) { this.arrivedHospitalAt = arrivedHospitalAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
    }
    public Double getDestinationLatitude() {
    return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }
}