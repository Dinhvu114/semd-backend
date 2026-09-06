package com.semd.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminPaymentResponse {
    private Long paymentId;
    private Integer missionId;

    private Integer driverId;
    private String driverName;

    private Integer providerId;
    private String providerName;

    private String serviceTypeCode;
    private BigDecimal distanceKm;

    private BigDecimal grossFare;
    private BigDecimal platformCommission;
    private BigDecimal driverAmount;
    private BigDecimal providerAmount;

    private String status;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public Integer getProviderId() { return providerId; }
    public void setProviderId(Integer providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getServiceTypeCode() { return serviceTypeCode; }
    public void setServiceTypeCode(String serviceTypeCode) { this.serviceTypeCode = serviceTypeCode; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public BigDecimal getGrossFare() { return grossFare; }
    public void setGrossFare(BigDecimal grossFare) { this.grossFare = grossFare; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getDriverAmount() { return driverAmount; }
    public void setDriverAmount(BigDecimal driverAmount) { this.driverAmount = driverAmount; }
    public BigDecimal getProviderAmount() { return providerAmount; }
    public void setProviderAmount(BigDecimal providerAmount) { this.providerAmount = providerAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}