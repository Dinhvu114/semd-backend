package com.semd.backend.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_zones")
public class OperationZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    @Column(name = "coverage_area", columnDefinition = "geometry(Polygon, 4326)")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Polygon coverageArea;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public Polygon getCoverageArea() { return coverageArea; }
    public void setCoverageArea(Polygon coverageArea) { this.coverageArea = coverageArea; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
