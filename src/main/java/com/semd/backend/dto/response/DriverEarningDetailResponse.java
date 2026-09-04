package com.semd.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DriverEarningDetailResponse {
    private Integer missionId;
    private Integer requestId;
    private Integer callId;

    private BigDecimal distanceKm;

    private BigDecimal grossFare;
    private BigDecimal platformCommission;
    private BigDecimal afterCommission;

    private BigDecimal driverAmount;
    private BigDecimal providerAmount;

    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paidAt;

    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public Integer getCallId() { return callId; }
    public void setCallId(Integer callId) { this.callId = callId; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public BigDecimal getGrossFare() { return grossFare; }
    public void setGrossFare(BigDecimal grossFare) { this.grossFare = grossFare; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getAfterCommission() { return afterCommission; }
    public void setAfterCommission(BigDecimal afterCommission) { this.afterCommission = afterCommission; }
    public BigDecimal getDriverAmount() { return driverAmount; }
    public void setDriverAmount(BigDecimal driverAmount) { this.driverAmount = driverAmount; }
    public BigDecimal getProviderAmount() { return providerAmount; }
    public void setProviderAmount(BigDecimal providerAmount) { this.providerAmount = providerAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}