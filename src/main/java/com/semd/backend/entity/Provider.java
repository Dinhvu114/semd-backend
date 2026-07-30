package com.semd.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "provider_name", nullable = false, length = 150)
    private String providerName;

    @Column(name = "provider_type", nullable = false, length = 20)
    private String providerType;

    @Column(name = "business_license", length = 100)
    private String businessLicense;

    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    @Column(name = "contact_address", length = 255)
    private String contactAddress;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }

    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
