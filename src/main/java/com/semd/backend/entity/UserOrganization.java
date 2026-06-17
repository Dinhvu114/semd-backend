package com.semd.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_organizations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "organization_type", "organization_id"})
})
public class UserOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "organization_type", nullable = false, length = 20)
    private String organizationType;

    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(name = "org_role", length = 30)
    private String orgRole = "STAFF";

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }

    public Integer getOrganizationId() { return organizationId; }
    public void setOrganizationId(Integer organizationId) { this.organizationId = organizationId; }

    public String getOrgRole() { return orgRole; }
    public void setOrgRole(String orgRole) { this.orgRole = orgRole; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
