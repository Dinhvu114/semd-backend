package com.semd.backend.service;

import com.semd.backend.entity.*;
import com.semd.backend.repository.PaymentTransactionRepository;
import com.semd.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    // ── Tỷ lệ chia tiền (giai đoạn demo) ────────────────────
    private static final BigDecimal PLATFORM_COMMISSION_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal DRIVER_SHARE_RATE        = BigDecimal.valueOf(0.20);

    private final PaymentTransactionRepository paymentRepo;
    private final UserRepository userRepository;
    private final FareCalculator fareCalculator;
    private final BillableDistanceResolver distanceResolver;

    public BillingService(PaymentTransactionRepository paymentRepo,
                          UserRepository userRepository,
                          FareCalculator fareCalculator,
                          BillableDistanceResolver distanceResolver) {
        this.paymentRepo = paymentRepo;
        this.userRepository = userRepository;
        this.fareCalculator = fareCalculator;
        this.distanceResolver = distanceResolver;
    }

    /**
     * Tạo PaymentTransaction PENDING cho mission vừa COMPLETED.
     * Chạy trong transaction riêng (REQUIRES_NEW) để lỗi billing
     * không rollback việc complete Mission.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createEstimatedPayment(DispatchMission mission) {

        // Tránh tạo trùng (idempotency)
        if (paymentRepo.findByMission_Id(mission.getId()).isPresent()) {
            log.info("Mission {} đã có payment, bỏ qua", mission.getId());
            return;
        }

        DispatchRequest request = mission.getRequest();
        if (request == null || request.getServiceType() == null) {
            log.warn("Mission {} thiếu serviceType, không tạo payment", mission.getId());
            return;
        }

        String serviceTypeCode = request.getServiceType().getTypeCode();

        // Lấy quãng đường billable, nếu không có thì bỏ qua (không phá mission)
        Optional<BigDecimal> distanceOpt = distanceResolver.resolveBillableDistanceKm(mission);
        if (distanceOpt.isEmpty()) {
            log.warn("KHÔNG TẠO PAYMENT cho mission {}: không xác định được quãng đường tính phí "
                            + "(có thể do simulation chưa có leg TO_HOSPITAL hoặc OSRM lỗi). "
                            + "Request sẽ COMPLETED nhưng chưa phát sinh cước — cần kiểm tra simulation của mission này.",
                    mission.getId());
            return;
        }
        BigDecimal distanceKm = distanceOpt.get();

        // Tính cước
        FareCalculator.FareBreakdown fare = fareCalculator.calculate(serviceTypeCode, distanceKm);

        // Chia tiền: platform 10%, driver 20% phần còn lại, provider 80% phần còn lại
        BigDecimal platformAmount = fare.totalFare()
                .multiply(PLATFORM_COMMISSION_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal afterCommission = fare.totalFare().subtract(platformAmount);
        BigDecimal driverAmount = afterCommission
                .multiply(DRIVER_SHARE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal providerAmount = afterCommission.subtract(driverAmount);

        // Resolve payer từ số điện thoại reporter
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

        paymentRepo.save(payment);

        log.info("Tạo payment PENDING cho mission {}: tổng={}, driver={}, provider={}",
                mission.getId(), fare.totalFare(), driverAmount, providerAmount);
    }
}