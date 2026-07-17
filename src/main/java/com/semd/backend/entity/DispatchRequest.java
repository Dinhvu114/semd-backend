package com.semd.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
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
    private OperationZone operationZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_dispatcher_id")
    private User createdByDispatcher;

    @Column(name = "urgency_level", length = 20)
    private String urgencyLevel = "MEDIUM";

    @Column(name = "target_location", nullable = false, columnDefinition = "geography(Point, 4326)")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Point targetLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private DispatchRequestStatus status = DispatchRequestStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_requirements", columnDefinition = "jsonb")
    private Map<String, Object> extendedRequirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "triage_level", length = 50)
    private String triageLevel;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public EmergencyCall getCall() { return call; }
    public void setCall(EmergencyCall call) { this.call = call; }

    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }

    public OperationZone getOperationZone() { return operationZone; }
    public void setOperationZone(OperationZone operationZone) { this.operationZone = operationZone; }

    public User getCreatedByDispatcher() { return createdByDispatcher; }
    public void setCreatedByDispatcher(User createdByDispatcher) { this.createdByDispatcher = createdByDispatcher; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public Point getTargetLocation() { return targetLocation; }
    public void setTargetLocation(Point targetLocation) { this.targetLocation = targetLocation; }

    public DispatchRequestStatus getStatus() { return status; }
    public void setStatus(DispatchRequestStatus status) { this.status = status; }

    public Map<String, Object> getExtendedRequirements() { return extendedRequirements; }
    public void setExtendedRequirements(Map<String, Object> extendedRequirements) { this.extendedRequirements = extendedRequirements; }

    public User getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(User confirmedBy) { this.confirmedBy = confirmedBy; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public BigDecimal getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; }

    public String getTriageLevel() { return triageLevel; }
    public void setTriageLevel(String triageLevel) { this.triageLevel = triageLevel; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getLongitude() {
        return targetLocation != null ? targetLocation.getX() : null;
    }

    public Double getLatitude() {
        return targetLocation != null ? targetLocation.getY() : null;
    }
}
