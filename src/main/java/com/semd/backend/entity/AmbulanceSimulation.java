package com.semd.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ambulance_simulations")
public class AmbulanceSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private DispatchMission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private DispatchResource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private MedicalHospital hospital;

    @Column(name = "source_type", length = 20)
    private String sourceType = "SIMULATION";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SimulationStatus status = SimulationStatus.READY;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 30, nullable = false)
    private SimulationPhase phase = SimulationPhase.TO_SCENE;

    @Column(name = "tick_interval_ms", nullable = false)
    private Integer tickIntervalMs = 1000;

    @Column(name = "speed_multiplier", nullable = false)
    private BigDecimal speedMultiplier = BigDecimal.ONE;

    @Column(name = "scene_wait_seconds")
    private Integer sceneWaitSeconds = 5;

    @Column(name = "route_index", nullable = false)
    private Integer routeIndex = 0;

    @Column(name = "elapsed_route_ms", nullable = false)
    private Long elapsedRouteMs = 0L;

    @Column(name = "distance_travelled_m")
    private BigDecimal distanceTravelledM = BigDecimal.ZERO;

    @Column(name = "last_tick_at")
    private OffsetDateTime lastTickAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "stopped_at")
    private OffsetDateTime stoppedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = OffsetDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DispatchMission getMission() { return mission; }
    public void setMission(DispatchMission mission) { this.mission = mission; }
    public DispatchResource getResource() { return resource; }
    public void setResource(DispatchResource resource) { this.resource = resource; }
    public MedicalHospital getHospital() { return hospital; }
    public void setHospital(MedicalHospital hospital) { this.hospital = hospital; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public SimulationStatus getStatus() { return status; }
    public void setStatus(SimulationStatus status) { this.status = status; }
    public SimulationPhase getPhase() { return phase; }
    public void setPhase(SimulationPhase phase) { this.phase = phase; }
    public Integer getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(Integer tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }
    public BigDecimal getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(BigDecimal speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public Integer getSceneWaitSeconds() { return sceneWaitSeconds; }
    public void setSceneWaitSeconds(Integer sceneWaitSeconds) { this.sceneWaitSeconds = sceneWaitSeconds; }
    public Integer getRouteIndex() { return routeIndex; }
    public void setRouteIndex(Integer routeIndex) { this.routeIndex = routeIndex; }
    public Long getElapsedRouteMs() { return elapsedRouteMs; }
    public void setElapsedRouteMs(Long elapsedRouteMs) { this.elapsedRouteMs = elapsedRouteMs; }
    public BigDecimal getDistanceTravelledM() { return distanceTravelledM; }
    public void setDistanceTravelledM(BigDecimal distanceTravelledM) { this.distanceTravelledM = distanceTravelledM; }
    public OffsetDateTime getLastTickAt() { return lastTickAt; }
    public void setLastTickAt(OffsetDateTime lastTickAt) { this.lastTickAt = lastTickAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(OffsetDateTime stoppedAt) { this.stoppedAt = stoppedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}