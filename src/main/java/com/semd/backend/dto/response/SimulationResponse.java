package com.semd.backend.dto.response;

import java.time.OffsetDateTime;

public class SimulationResponse {
    private Long id;
    private Integer missionId;
    private Integer resourceId;
    private Integer hospitalId;
    private String sourceType;
    private String status;
    private String phase;
    private Integer tickIntervalMs;
    private Double speedMultiplier;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String errorCode;
    private String errorMessage;
    private Double currentLongitude;
    private Double currentLatitude;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
    public Integer getHospitalId() { return hospitalId; }
    public void setHospitalId(Integer hospitalId) { this.hospitalId = hospitalId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public Integer getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(Integer tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }
    public Double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(Double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
}