package com.semd.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateSimulationRequest {

    @NotNull(message = "missionId không được để trống")
    private Integer missionId;

    @NotNull(message = "hospitalId không được để trống")
    private Integer hospitalId;

    @Min(value = 250, message = "tickIntervalMs phải >= 250ms")
    private Integer tickIntervalMs = 1000;

    @DecimalMin(value = "0.1", message = "speedMultiplier phải > 0")
    private Double speedMultiplier = 10.0;

    @Min(value = 0, message = "sceneWaitSeconds phải >= 0")
    private Integer sceneWaitSeconds = 5;

    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public Integer getHospitalId() { return hospitalId; }
    public void setHospitalId(Integer hospitalId) { this.hospitalId = hospitalId; }
    public Integer getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(Integer tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }
    public Double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(Double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public Integer getSceneWaitSeconds() { return sceneWaitSeconds; }
    public void setSceneWaitSeconds(Integer sceneWaitSeconds) { this.sceneWaitSeconds = sceneWaitSeconds; }
}