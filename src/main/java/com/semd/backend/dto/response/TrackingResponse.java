package com.semd.backend.dto.response;

import java.time.OffsetDateTime;

public class TrackingResponse {
    private Long simulationId;
    private Integer missionId;
    private Integer resourceId;
    private String status;
    private String phase;
    private String sourceType;
    private Double currentLongitude;
    private Double currentLatitude;
    private Double progressPercent;
    private Double remainingDistanceMeters;
    private Double etaSeconds;
    private OffsetDateTime lastUpdatedAt;

    // Getters & Setters
    public Long getSimulationId() { return simulationId; }
    public void setSimulationId(Long simulationId) { this.simulationId = simulationId; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public Integer getResourceId() { return resourceId; }
    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
    public Double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Double progressPercent) { this.progressPercent = progressPercent; }
    public Double getRemainingDistanceMeters() { return remainingDistanceMeters; }
    public void setRemainingDistanceMeters(Double remainingDistanceMeters) { this.remainingDistanceMeters = remainingDistanceMeters; }
    public Double getEtaSeconds() { return etaSeconds; }
    public void setEtaSeconds(Double etaSeconds) { this.etaSeconds = etaSeconds; }
    public OffsetDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(OffsetDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}