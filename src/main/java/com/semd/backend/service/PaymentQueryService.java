package com.semd.backend.service;

import com.semd.backend.dto.response.DriverEarningResponse;
import com.semd.backend.dto.response.DriverEarningSummaryResponse;
import com.semd.backend.dto.response.PaymentDetailResponse;
import com.semd.backend.entity.PaymentTransaction;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PaymentQueryService {

    private final PaymentTransactionRepository paymentRepo;

    public PaymentQueryService(PaymentTransactionRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    // ── REPORTER ──────────────────────────────────────────
    public List<PaymentDetailResponse> getMyPayments(Integer payerId) {
        return paymentRepo.findByPayer_IdOrderByCreatedAtDesc(payerId)
                .stream().map(this::toDetailResponse).toList();
    }

    public PaymentDetailResponse getMyPaymentById(Integer payerId, Long paymentId) {
        PaymentTransaction payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch id: " + paymentId));

        if (payment.getPayer() == null || !payment.getPayer().getId().equals(payerId)) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch id: " + paymentId);
        }
        return toDetailResponse(payment);
    }

    private PaymentDetailResponse toDetailResponse(PaymentTransaction p) {
        PaymentDetailResponse res = new PaymentDetailResponse();
        res.setPaymentId(p.getId());
        res.setMissionId(p.getMission() != null ? p.getMission().getId() : null);
        res.setStatus(p.getStatus());
        res.setServiceTypeCode(p.getServiceTypeCode());
        res.setDestinationName(p.getMission() != null ? p.getMission().getDestinationName() : null);
        res.setDriverName(p.getDriver() != null ? p.getDriver().getFullName() : null);
        res.setCompletedAt(p.getMission() != null ? p.getMission().getCompletedAt() : null);
        res.setBillableDistanceKm(p.getBillableDistanceKm());
        res.setBaseFare(p.getBaseFare());
        res.setPricePerKm(p.getPricePerKm());
        res.setDistanceFare(p.getDistanceFare());
        res.setTotalAmount(p.getAmount());
        return res;
    }

    // ── DRIVER ────────────────────────────────────────────
    public List<DriverEarningResponse> getMyEarnings(Integer driverId) {
        return paymentRepo.findByDriver_IdOrderByCreatedAtDesc(driverId)
                .stream().map(this::toEarningResponse).toList();
    }

    public DriverEarningSummaryResponse getMyEarningSummary(Integer driverId) {
        List<PaymentTransaction> payments = paymentRepo.findByDriver_IdOrderByCreatedAtDesc(driverId);

        BigDecimal pending = payments.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.getStatus()))
                .map(PaymentTransaction::getDriverAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paid = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .map(PaymentTransaction::getDriverAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
}