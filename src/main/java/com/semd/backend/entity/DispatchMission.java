package com.semd.backend.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.LineString;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_missions")
public class DispatchMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private DispatchRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private DispatchResource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private MedicalHospital destination;

    @Column(name = "destination_name", length = 255)
    private String destinationName;

    @Column(name = "status", length = 20)
    private String status = "DISPATCHED";

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "en_route_at")
    private LocalDateTime enRouteAt;

    @Column(name = "on_scene_at")
    private LocalDateTime onSceneAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "route_geometry", columnDefinition = "geography(LineString, 4326)")
    private LineString routeGeometry;

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public DispatchRequest getRequest() { return request; }
    public void setRequest(DispatchRequest request) { this.request = request; }

    public DispatchResource getResource() { return resource; }
    public void setResource(DispatchResource resource) { this.resource = resource; }

    public MedicalHospital getDestination() { return destination; }
    public void setDestination(MedicalHospital destination) { this.destination = destination; }

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

    public LocalDateTime getOnSceneAt() { return onSceneAt; }
    public void setOnSceneAt(LocalDateTime onSceneAt) { this.onSceneAt = onSceneAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LineString getRouteGeometry() { return routeGeometry; }
    public void setRouteGeometry(LineString routeGeometry) { this.routeGeometry = routeGeometry; }
}
