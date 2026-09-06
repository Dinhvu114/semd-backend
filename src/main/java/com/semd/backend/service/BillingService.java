package com.semd.backend.service;

import com.semd.backend.entity.*;
import com.semd.backend.repository.PaymentTransactionRepository;
import com.semd.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private static final BigDecimal PLATFORM_COMMISSION_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal DRIVER_SHARE_RATE        = BigDecimal.valueOf(0.20);

    private final PaymentTransactionRepository paymentRepo;
    private final UserRepository userRepository;
    private final FareCalculator fareCalculator;
    private final BillableDistanceResolver distanceResolver;
    private final SimpMessagingTemplate messagingTemplate; // ← THÊM

    public BillingService(PaymentTransactionRepository paymentRepo,
                          UserRepository userRepository,
                          FareCalculator fareCalculator,
                          BillableDistanceResolver distanceResolver,
                          SimpMessagingTemplate messagingTemplate) { // ← THÊM
        this.paymentRepo = paymentRepo;
        this.userRepository = userRepository;
        this.fareCalculator = fareCalculator;
        this.distanceResolver = distanceResolver;
        this.messagingTemplate = messagingTemplate; // ← THÊM
    }

    /**
     * Tạo PaymentTransaction PENDING cho mission vừa COMPLETED.
     * Trả về PaymentTransaction (hoặc null nếu không tạo được) để caller
     * có thể gửi thông báo/log tiếp theo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction createEstimatedPayment(DispatchMission mission) {

        // Tránh tạo trùng (idempotency)
        Optional<PaymentTransaction> existing = paymentRepo.findByMission_Id(mission.getId());
        if (existing.isPresent()) {
            log.info("Mission {} đã có payment, bỏ qua", mission.getId());
            return existing.get();
        }

        DispatchRequest request = mission.getRequest();
        if (request == null || request.getServiceType() == null) {
            log.warn("KHÔNG TẠO PAYMENT cho mission {}: thiếu serviceType", mission.getId());
            return null;
        }

        String serviceTypeCode = request.getServiceType().getTypeCode();

        Optional<BigDecimal> distanceOpt = distanceResolver.resolveBillableDistanceKm(mission);
        if (distanceOpt.isEmpty()) {
            log.warn("KHÔNG TẠO PAYMENT cho mission {}: không xác định được quãng đường tính phí "
                            + "(có thể do simulation chưa có leg TO_HOSPITAL hoặc OSRM lỗi). "
                            + "Request sẽ COMPLETED nhưng chưa phát sinh cước — cần kiểm tra simulation của mission này.",
                    mission.getId());
            return null;
        }
        BigDecimal distanceKm = distanceOpt.get();

        FareCalculator.FareBreakdown fare = fareCalculator.calculate(serviceTypeCode, distanceKm);

        BigDecimal platformAmount = fare.totalFare()
                .multiply(PLATFORM_COMMISSION_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal afterCommission = fare.totalFare().subtract(platformAmount);
        BigDecimal driverAmount = afterCommission
                .multiply(DRIVER_SHARE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal providerAmount = afterCommission.subtract(driverAmount);

        User payer = null;
        if (request.getCall() != null && request.getCall().getReporterPhone() != null) {
            payer = userRepository.findByPhoneNumber(request.getCall().getReporterPhone())
                    .orElse(null);
        }

        DispatchResource resource = mission.getResource();
        Provider provider = resource != null ? resource.getProvider() : null;
        User driver = resource != null ? resource.getCurrentDriver() : null;

        PaymentTransaction payment = new PaymentTransaction();
        payment.setMission(mission);
        payment.setPayer(payer);
        payment.setProvider(provider);
        payment.setDriver(driver);
        payment.setAmount(fare.totalFare());
        payment.setCommissionAmount(platformAmount);
        payment.setStatus("PENDING");
        payment.setServiceTypeCode(serviceTypeCode);
        payment.setBillableDistanceKm(distanceKm);
        payment.setBaseFare(fare.baseFare());
        payment.setPricePerKm(fare.pricePerKm());
        payment.setDistanceFare(fare.distanceFare());
        payment.setDriverAmount(driverAmount);
        payment.setProviderAmount(providerAmount);

        PaymentTransaction saved = paymentRepo.save(payment);

        log.info("Tạo payment PENDING cho mission {}: tổng={}, driver={}, provider={}",
                mission.getId(), fare.totalFare(), driverAmount, providerAmount);

        // ── THÊM MỚI: Thông báo Reporter hóa đơn đã sẵn sàng ──
        notifyPaymentReady(saved);

        return saved;
    }

    private void notifyPaymentReady(PaymentTransaction payment) {
        if (payment.getPayer() == null) {
            log.warn("Payment {} không có payer, bỏ qua thông báo PAYMENT_READY", payment.getId());
            return;
        }

        try {
            Integer callId = payment.getMission() != null
                    && payment.getMission().getRequest() != null
                    && payment.getMission().getRequest().getCall() != null
                    ? payment.getMission().getRequest().getCall().getId() : null;

            messagingTemplate.convertAndSend(
                    "/topic/reporter/" + payment.getPayer().getId(),
                    (Object) Map.of(
                            "event", "PAYMENT_READY",
                            "callId", callId != null ? callId : 0,
                            "missionId", payment.getMission().getId(),
                            "paymentId", payment.getId(),
                            "status", "PENDING",
                            "totalAmount", payment.getAmount(),
                            "message", "Ca cấp cứu đã hoàn thành. Vui lòng thanh toán chi phí dịch vụ."
                    )
            );
            log.info("Đã gửi PAYMENT_READY cho payer {} (payment {})",
                    payment.getPayer().getId(), payment.getId());
        } catch (Exception e) {
            log.error("Gửi thông báo PAYMENT_READY thất bại cho payment {}: {}",
                    payment.getId(), e.getMessage());
        }
    }
}