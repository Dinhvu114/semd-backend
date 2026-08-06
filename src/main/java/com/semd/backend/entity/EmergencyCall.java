package com.semd.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "emergency_calls")
public class EmergencyCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatcher_id")
    private User dispatcher;

    @Column(name = "reporter_phone", nullable = false, length = 15)
    private String reporterPhone;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "call_start_time", nullable = false)
    private LocalDateTime callStartTime;

    @Column(name = "call_duration")
    private Integer callDuration;

    @Column(name = "audio_url", length = 512)
    private String audioUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "ai_transcript", columnDefinition = "text")
    private String aiTranscript;

    @Column(name = "ai_urgency_prediction", length = 20)
    private String aiUrgencyPrediction;

    @Column(name = "ai_confidence_score", precision = 5, scale = 2)
    private BigDecimal aiConfidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 20)
    private EmergencyCallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmergencyCallStatus status = EmergencyCallStatus.RECEIVED;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    @JsonIgnore
    private Point location;

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getDispatcher() { return dispatcher; }
    public void setDispatcher(User dispatcher) { this.dispatcher = dispatcher; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public LocalDateTime getCallStartTime() { return callStartTime; }
    public void setCallStartTime(LocalDateTime callStartTime) { this.callStartTime = callStartTime; }

    public Integer getCallDuration() { return callDuration; }
    public void setCallDuration(Integer callDuration) { this.callDuration = callDuration; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAiTranscript() { return aiTranscript; }
    public void setAiTranscript(String aiTranscript) { this.aiTranscript = aiTranscript; }

    public String getAiUrgencyPrediction() { return aiUrgencyPrediction; }
    public void setAiUrgencyPrediction(String aiUrgencyPrediction) { this.aiUrgencyPrediction = aiUrgencyPrediction; }

    public BigDecimal getAiConfidenceScore() { return aiConfidenceScore; }
    public void setAiConfidenceScore(BigDecimal aiConfidenceScore) { this.aiConfidenceScore = aiConfidenceScore; }

    public EmergencyCallType getCallType() { return callType; }
    public void setCallType(EmergencyCallType callType) { this.callType = callType; }

    public EmergencyCallStatus getStatus() { return status; }
    public void setStatus(EmergencyCallStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }

    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }
}
