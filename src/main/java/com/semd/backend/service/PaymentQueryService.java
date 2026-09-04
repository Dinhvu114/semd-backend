package com.semd.backend.service;

import com.semd.backend.dto.response.*;
import com.semd.backend.entity.PaymentTransaction;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentQueryService {

    private final PaymentTransactionRepository paymentRepo;

    public PaymentQueryService(PaymentTransactionRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    // ══════════════════════════════════════════════════════
    // REPORTER
    // ══════════════════════════════════════════════════════
    public List<PaymentDetailResponse> getMyPayments(Integer payerId) {
        return paymentRepo.findByPayer_IdOrderByCreatedAtDesc(payerId)
                .stream().map(this::toDetailResponse).toList();
    }

    public PaymentDetailResponse getMyPaymentById(Integer payerId, Long paymentId) {
        PaymentTransaction payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch id: " + paymentId));
        assertOwnedByPayer(payment, payerId);
        return toDetailResponse(payment);
    }

    // ── THÊM MỚI: theo callId ────────────────────────────
    public PaymentDetailResponse getMyPaymentByCallId(Integer payerId, Integer callId) {
        PaymentTransaction payment = paymentRepo.findByMission_Request_Call_Id(callId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có giao dịch thanh toán cho cuộc gọi id: " + callId));
        assertOwnedByPayer(payment, payerId);
        return toDetailResponse(payment);
    }

    // ── THÊM MỚI: Reporter xác nhận thanh toán ────────────
    @Transactional
    public PaymentDetailResponse payPayment(Integer payerId, Long paymentId, String paymentMethod) {
        PaymentTransaction payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch id: " + paymentId));
        assertOwnedByPayer(payment, payerId);

        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            throw new BusinessConflictException(
                    "Giao dịch đã ở trạng thái " + payment.getStatus() + ", không thể thanh toán lại");
        }

        payment.setPaymentMethod(paymentMethod);
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        payment.setExternalTransactionId(generateTransactionRef());

        paymentRepo.save(payment);
        return toDetailResponse(payment);
    }

    private void assertOwnedByPayer(PaymentTransaction payment, Integer payerId) {
        if (payment.getPayer() == null || !payment.getPayer().getId().equals(payerId)) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch");
        }
    }

    private String generateTransactionRef() {
        return "TXN-" + LocalDateTime.now().toLocalDate() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentDetailResponse toDetailResponse(PaymentTransaction p) {
        PaymentDetailResponse res = new PaymentDetailResponse();
        res.setPaymentId(p.getId());
        res.setStatus(p.getStatus());

        if (p.getMission() != null) {
            var mission = p.getMission();
            res.setMissionId(mission.getId());
            res.setCompletedAt(mission.getCompletedAt());
            res.setHospitalAddress(mission.getDestinationName());

            if (mission.getRequest() != null) {
                var request = mission.getRequest();
                res.setRequestId(request.getId());
                res.setPickupAddress(request.getConfirmedAddress());

                if (request.getCall() != null) {
                    res.setCallId(request.getCall().getId());
                    res.setPatientName(request.getCall().getReporterName());
                    res.setPatientPhone(request.getCall().getReporterPhone());
                }
            }

            if (mission.getResource() != null) {
                res.setLicensePlate(mission.getResource().getResourceCode());
            }
        }

        res.setDriverName(p.getDriver() != null ? p.getDriver().getFullName() : null);
        res.setServiceTypeCode(p.getServiceTypeCode());
        res.setBillableDistanceKm(p.getBillableDistanceKm());
        res.setBaseFare(p.getBaseFare());
        res.setPricePerKm(p.getPricePerKm());
        res.setDistanceFare(p.getDistanceFare());
        res.setTotalAmount(p.getAmount());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setExternalTransactionId(p.getExternalTransactionId());
        res.setCreatedAt(p.getCreatedAt());
        res.setPaidAt(p.getPaidAt());
        return res;
    }

    // ══════════════════════════════════════════════════════
    // DRIVER
    // ══════════════════════════════════════════════════════
    public List<DriverEarningResponse> getMyEarnings(Integer driverId) {
        return paymentRepo.findByDriver_IdOrderByCreatedAtDesc(driverId)
                .stream().map(this::toEarningResponse).toList();
    }

    public DriverEarningSummaryResponse getMyEarningSummary(Integer driverId) {
        List<PaymentTransaction> payments = paymentRepo.findByDriver_IdOrderByCreatedAtDesc(driverId);

        BigDecimal pending = sumDriverAmount(payments, "PENDING");
        BigDecimal paid = sumDriverAmount(payments, "SUCCESS");

        int count = payments.size();
        BigDecimal total = pending.add(paid);
        BigDecimal average = count > 0
                ? total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        DriverEarningSummaryResponse res = new DriverEarningSummaryResponse();
        res.setPendingEarnings(pending);
        res.setPaidEarnings(paid);
        res.setMissionCount(count);
        res.setAveragePerMission(average);
        return res;
    }

    // ── THÊM MỚI: chi tiết earning theo missionId ─────────
    public DriverEarningDetailResponse getMyEarningByMission(Integer driverId, Integer missionId) {
        PaymentTransaction payment = paymentRepo.findByMission_Id(missionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có giao dịch cho nhiệm vụ id: " + missionId));
        assertOwnedByDriver(payment, driverId);
        return toEarningDetailResponse(payment);
    }

    // ── THÊM MỚI: Driver xác nhận thu tiền mặt ────────────
    @Transactional
    public DriverEarningDetailResponse collectCash(Integer driverId, Integer missionId) {
        PaymentTransaction payment = paymentRepo.findByMission_Id(missionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có giao dịch cho nhiệm vụ id: " + missionId));
        assertOwnedByDriver(payment, driverId);

        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            throw new BusinessConflictException(
                    "Giao dịch đã ở trạng thái " + payment.getStatus() + ", không thể xác nhận thu tiền mặt");
        }

        payment.setPaymentMethod("CASH");
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());

        paymentRepo.save(payment);
        return toEarningDetailResponse(payment);
    }

    private void assertOwnedByDriver(PaymentTransaction payment, Integer driverId) {
        if (payment.getDriver() == null || !payment.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Nhiệm vụ không thuộc tài xế hiện tại");
        }
    }

    private BigDecimal sumDriverAmount(List<PaymentTransaction> payments, String status) {
        return payments.stream()
                .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                .map(PaymentTransaction::getDriverAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DriverEarningResponse toEarningResponse(PaymentTransaction p) {
        DriverEarningResponse res = new DriverEarningResponse();
        res.setMissionId(p.getMission() != null ? p.getMission().getId() : null);
        res.setServiceTypeCode(p.getServiceTypeCode());
        res.setGrossFare(p.getAmount());
        res.setPlatformCommission(p.getCommissionAmount());
        BigDecimal afterCommission = p.getAmount() != null && p.getCommissionAmount() != null
                ? p.getAmount().subtract(p.getCommissionAmount()) : null;
        res.setAfterCommission(afterCommission);
        res.setDriverAmount(p.getDriverAmount());
        res.setProviderAmount(p.getProviderAmount());
        res.setPaymentStatus(p.getStatus());
        return res;
    }

    private DriverEarningDetailResponse toEarningDetailResponse(PaymentTransaction p) {
        DriverEarningDetailResponse res = new DriverEarningDetailResponse();
        res.setMissionId(p.getMission() != null ? p.getMission().getId() : null);

        if (p.getMission() != null && p.getMission().getRequest() != null) {
            res.setRequestId(p.getMission().getRequest().getId());
            if (p.getMission().getRequest().getCall() != null) {
                res.setCallId(p.getMission().getRequest().getCall().getId());
            }
        }

        res.setDistanceKm(p.getBillableDistanceKm());
        res.setGrossFare(p.getAmount());
        res.setPlatformCommission(p.getCommissionAmount());
        BigDecimal afterCommission = p.getAmount() != null && p.getCommissionAmount() != null
                ? p.getAmount().subtract(p.getCommissionAmount()) : null;
        res.setAfterCommission(afterCommission);
        res.setDriverAmount(p.getDriverAmount());
        res.setProviderAmount(p.getProviderAmount());
        res.setPaymentStatus(p.getStatus());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setPaidAt(p.getPaidAt());
        return res;
    }

    // ══════════════════════════════════════════════════════
    // PROVIDER
    // ══════════════════════════════════════════════════════
    public List<ProviderPaymentResponse> getProviderPayments(Integer providerId) {
        return paymentRepo.findByProvider_IdOrderByCreatedAtDesc(providerId)
                .stream().map(this::toProviderPaymentResponse).toList();
    }

    private ProviderPaymentResponse toProviderPaymentResponse(PaymentTransaction p) {
        ProviderPaymentResponse res = new ProviderPaymentResponse();
        res.setPaymentId(p.getId());
        res.setMissionId(p.getMission() != null ? p.getMission().getId() : null);
        res.setServiceTypeCode(p.getServiceTypeCode());
        res.setDistanceKm(p.getBillableDistanceKm());
        res.setAmount(p.getAmount());
        res.setPlatformCommission(p.getCommissionAmount());
        res.setDriverAmount(p.getDriverAmount());
        res.setProviderAmount(p.getProviderAmount());
        res.setStatus(p.getStatus());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setPaidAt(p.getPaidAt());
        return res;
    }
}