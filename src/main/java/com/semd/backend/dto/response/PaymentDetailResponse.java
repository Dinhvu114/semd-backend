package com.semd.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDetailResponse {
    private Long paymentId;
    private Integer missionId;
    private String status;

    private String serviceTypeCode;
    private String destinationName;
    private String driverName;
    private LocalDateTime completedAt;

    private BigDecimal billableDistanceKm;
    private BigDecimal baseFare;
    private BigDecimal pricePerKm;
    private BigDecimal distanceFare;
    private BigDecimal totalAmount;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getServiceTypeCode() { return serviceTypeCode; }
    public void setServiceTypeCode(String serviceTypeCode) { this.serviceTypeCode = serviceTypeCode; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public BigDecimal getBillableDistanceKm() { return billableDistanceKm; }
    public void setBillableDistanceKm(BigDecimal billableDistanceKm) { this.billableDistanceKm = billableDistanceKm; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(BigDecimal pricePerKm) { this.pricePerKm = pricePerKm; }
    public BigDecimal getDistanceFare() { return distanceFare; }
    public void setDistanceFare(BigDecimal distanceFare) { this.distanceFare = distanceFare; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}