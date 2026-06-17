package com.semd.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "dispatch_resources")
public class DispatchResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "resource_code", nullable = false, unique = true, length = 50)
    private String resourceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_type_id")
    private ServiceType resourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edge_node_id")
    private EdgeNode edgeNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_driver_id")
    private User currentDriver;

    @Column(name = "status", length = 20)
    private String status = "AVAILABLE";

    @Column(name = "current_location", columnDefinition = "geography(Point, 4326)")
    private Point currentLocation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_attributes", columnDefinition = "jsonb")
    private Map<String, Object> extendedAttributes;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public ServiceType getResourceType() { return resourceType; }
    public void setResourceType(ServiceType resourceType) { this.resourceType = resourceType; }

    public EdgeNode getEdgeNode() { return edgeNode; }
    public void setEdgeNode(EdgeNode edgeNode) { this.edgeNode = edgeNode; }

    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }

    public User getCurrentDriver() { return currentDriver; }
    public void setCurrentDriver(User currentDriver) { this.currentDriver = currentDriver; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Point getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Point currentLocation) { this.currentLocation = currentLocation; }

    public Map<String, Object> getExtendedAttributes() { return extendedAttributes; }
    public void setExtendedAttributes(Map<String, Object> extendedAttributes) { this.extendedAttributes = extendedAttributes; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
