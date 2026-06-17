package com.semd.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "dispatch_requests")
public class DispatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id")
    private EmergencyCall call;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edge_node_id")
    private EdgeNode edgeNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_dispatcher_id")
    private User createdByDispatcher;

    @Column(name = "urgency_level", length = 20)
    private String urgencyLevel = "MEDIUM";

    @Column(name = "target_location", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point targetLocation;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_requirements", columnDefinition = "jsonb")
    private Map<String, Object> extendedRequirements;

    @Column(name = "is_synced_to_cloud")
    private Boolean isSyncedToCloud = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public EmergencyCall getCall() { return call; }
    public void setCall(EmergencyCall call) { this.call = call; }

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public EdgeNode getEdgeNode() { return edgeNode; }
    public void setEdgeNode(EdgeNode edgeNode) { this.edgeNode = edgeNode; }

    public User getCreatedByDispatcher() { return createdByDispatcher; }
    public void setCreatedByDispatcher(User createdByDispatcher) { this.createdByDispatcher = createdByDispatcher; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public Point getTargetLocation() { return targetLocation; }
    public void setTargetLocation(Point targetLocation) { this.targetLocation = targetLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getExtendedRequirements() { return extendedRequirements; }
    public void setExtendedRequirements(Map<String, Object> extendedRequirements) { this.extendedRequirements = extendedRequirements; }

    public Boolean getIsSyncedToCloud() { return isSyncedToCloud; }
    public void setIsSyncedToCloud(Boolean isSyncedToCloud) { this.isSyncedToCloud = isSyncedToCloud; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
