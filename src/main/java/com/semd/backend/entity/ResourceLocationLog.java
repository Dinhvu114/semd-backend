package com.semd.backend.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_location_logs")
public class ResourceLocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private DispatchResource resource;

    // ── THÊM MỚI ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private DispatchMission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id")
    private AmbulanceSimulation simulation;

    @Column(name = "source_type", length = 20)
    private String sourceType = "SIMULATION";

    @Column(name = "sequence_no")
    private Long sequenceNo;
    // ── HẾT ──────────────────────────────────────

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "speed", precision = 5, scale = 2)
    private BigDecimal speed;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt = LocalDateTime.now();

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DispatchResource getResource() { return resource; }
    public void setResource(DispatchResource resource) { this.resource = resource; }

    public DispatchMission getMission() { return mission; }
    public void setMission(DispatchMission mission) { this.mission = mission; }

    public AmbulanceSimulation getSimulation() { return simulation; }
    public void setSimulation(AmbulanceSimulation simulation) { this.simulation = simulation; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public BigDecimal getSpeed() { return speed; }
    public void setSpeed(BigDecimal speed) { this.speed = speed; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
