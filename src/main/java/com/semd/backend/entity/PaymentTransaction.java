package com.semd.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private DispatchMission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_user_id")
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private IntegrationPartner partner;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "external_transaction_id", length = 255)
    private String externalTransactionId;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "service_type_code", length = 30)
    private String serviceTypeCode;

    @Column(name = "billable_distance_km", precision = 10, scale = 2)
    private BigDecimal billableDistanceKm;

    @Column(name = "base_fare", precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "price_per_km", precision = 12, scale = 2)
    private BigDecimal pricePerKm;

    @Column(name = "distance_fare", precision = 12, scale = 2)
    private BigDecimal distanceFare;

    @Column(name = "driver_amount", precision = 12, scale = 2)
    private BigDecimal driverAmount;

    @Column(name = "provider_amount", precision = 12, scale = 2)
    private BigDecimal providerAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DispatchMission getMission() { return mission; }
    public void setMission(DispatchMission mission) { this.mission = mission; }
    public User getPayer() { return payer; }
    public void setPayer(User payer) { this.payer = payer; }
    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public IntegrationPartner getPartner() { return partner; }
    public void setPartner(IntegrationPartner partner) { this.partner = partner; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getServiceTypeCode() { return serviceTypeCode; }
    public void setServiceTypeCode(String serviceTypeCode) { this.serviceTypeCode = serviceTypeCode; }
    public BigDecimal getBillableDistanceKm() { return billableDistanceKm; }
    public void setBillableDistanceKm(BigDecimal billableDistanceKm) { this.billableDistanceKm = billableDistanceKm; }
    public BigDecimal getBaseFare() { return baseFare; }
    public void setBaseFare(BigDecimal baseFare) { this.baseFare = baseFare; }
    public BigDecimal getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(BigDecimal pricePerKm) { this.pricePerKm = pricePerKm; }
    public BigDecimal getDistanceFare() { return distanceFare; }
    public void setDistanceFare(BigDecimal distanceFare) { this.distanceFare = distanceFare; }
    public BigDecimal getDriverAmount() { return driverAmount; }
    public void setDriverAmount(BigDecimal driverAmount) { this.driverAmount = driverAmount; }
    public BigDecimal getProviderAmount() { return providerAmount; }
    public void setProviderAmount(BigDecimal providerAmount) { this.providerAmount = providerAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}