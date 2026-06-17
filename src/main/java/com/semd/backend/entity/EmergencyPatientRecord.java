package com.semd.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "emergency_patient_records")
public class EmergencyPatientRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private DispatchMission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(name = "patient_name", length = 100)
    private String patientName = "Chưa rõ danh tính";

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "triage_color", length = 10)
    private String triageColor = "YELLOW";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vital_signs", columnDefinition = "jsonb")
    private Map<String, Object> vitalSigns;

    @Column(name = "clinical_note", columnDefinition = "text")
    private String clinicalNote;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public DispatchMission getMission() { return mission; }
    public void setMission(DispatchMission mission) { this.mission = mission; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getTriageColor() { return triageColor; }
    public void setTriageColor(String triageColor) { this.triageColor = triageColor; }

    public Map<String, Object> getVitalSigns() { return vitalSigns; }
    public void setVitalSigns(Map<String, Object> vitalSigns) { this.vitalSigns = vitalSigns; }

    public String getClinicalNote() { return clinicalNote; }
    public void setClinicalNote(String clinicalNote) { this.clinicalNote = clinicalNote; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
