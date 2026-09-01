package com.semd.backend.dto.response;

import java.math.BigDecimal;

public class DriverEarningResponse {
    private Integer missionId;
    private String serviceTypeCode;
    private BigDecimal grossFare;
    private BigDecimal platformCommission;
    private BigDecimal afterCommission;
    private BigDecimal driverAmount;
    private BigDecimal providerAmount;
    private String paymentStatus;

    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public String getServiceTypeCode() { return serviceTypeCode; }
    public void setServiceTypeCode(String serviceTypeCode) { this.serviceTypeCode = serviceTypeCode; }
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
}