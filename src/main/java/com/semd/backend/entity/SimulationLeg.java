package com.semd.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "simulation_legs")
public class SimulationLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private AmbulanceSimulation simulation;

    @Enumerated(EnumType.STRING)
    @Column(name = "leg_type", length = 30, nullable = false)
    private LegType legType;

    @Column(name = "sequence_no", nullable = false)
    private Short sequenceNo;

    @Column(name = "route_payload", columnDefinition = "jsonb")
    private String routePayload; // lưu dạng JSON string

    @Column(name = "distance_m")
    private BigDecimal distanceM;

    @Column(name = "duration_s")
    private BigDecimal durationS;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AmbulanceSimulation getSimulation() { return simulation; }
    public void setSimulation(AmbulanceSimulation simulation) { this.simulation = simulation; }
    public LegType getLegType() { return legType; }
    public void setLegType(LegType legType) { this.legType = legType; }
    public Short getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Short sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getRoutePayload() { return routePayload; }
    public void setRoutePayload(String routePayload) { this.routePayload = routePayload; }
    public BigDecimal getDistanceM() { return distanceM; }
    public void setDistanceM(BigDecimal distanceM) { this.distanceM = distanceM; }
    public BigDecimal getDurationS() { return durationS; }
    public void setDurationS(BigDecimal durationS) { this.durationS = durationS; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}