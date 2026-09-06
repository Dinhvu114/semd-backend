package com.semd.backend.service;

import com.semd.backend.entity.*;
import com.semd.backend.repository.PaymentTransactionRepository;
import com.semd.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock PaymentTransactionRepository paymentRepo;
    @Mock UserRepository userRepository;
    @Mock FareCalculator fareCalculator;
    @Mock BillableDistanceResolver distanceResolver;
    @InjectMocks BillingService billingService;

    @Test
    void createEstimatedPayment_shouldCreatePendingAndSplit10_18_72() {
        DispatchMission mission = missionGraph(101, "BLS", "0901000001", 7, 8);
        User payer = new User();
        payer.setId(9);

        BigDecimal distance = new BigDecimal("10.00");
        FareCalculator.FareBreakdown fare = new FareCalculator.FareBreakdown(
                "BLS", distance,
                new BigDecimal("200000"), new BigDecimal("40000"),
                new BigDecimal("400000"), new BigDecimal("600000"));

        when(paymentRepo.findByMission_Id(101)).thenReturn(Optional.empty());
        when(distanceResolver.resolveBillableDistanceKm(mission)).thenReturn(Optional.of(distance));
        when(fareCalculator.calculate("BLS", distance)).thenReturn(fare);
        when(userRepository.findByPhoneNumber("0901000001")).thenReturn(Optional.of(payer));

        billingService.createEstimatedPayment(mission);

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepo).save(captor.capture());
        PaymentTransaction saved = captor.getValue();

        assertEquals("PENDING", saved.getStatus());
        assertEquals(0, saved.getAmount().compareTo(new BigDecimal("600000")));
        assertEquals(0, saved.getCommissionAmount().compareTo(new BigDecimal("60000"))); // 10%
        assertEquals(0, saved.getDriverAmount().compareTo(new BigDecimal("108000")));    // 18% gross
        assertEquals(0, saved.getProviderAmount().compareTo(new BigDecimal("432000")));  // 72% gross
        assertEquals(0, saved.getCommissionAmount().add(saved.getDriverAmount())
                .add(saved.getProviderAmount()).compareTo(saved.getAmount()));
        assertSame(payer, saved.getPayer());
        assertEquals(7, saved.getProvider().getId());
        assertEquals(8, saved.getDriver().getId());
    }

    @Test
    void createEstimatedPayment_whenMissionAlreadyHasPayment_shouldBeIdempotent() {
        DispatchMission mission = new DispatchMission();
        mission.setId(101);
        when(paymentRepo.findByMission_Id(101)).thenReturn(Optional.of(new PaymentTransaction()));

        billingService.createEstimatedPayment(mission);

        verify(paymentRepo, never()).save(any());
        verifyNoInteractions(distanceResolver, fareCalculator, userRepository);
    }

    @Test
    void createEstimatedPayment_whenServiceTypeMissing_shouldNotCreatePayment() {
        DispatchMission mission = new DispatchMission();
        mission.setId(102);
        DispatchRequest request = new DispatchRequest();
        mission.setRequest(request);
        when(paymentRepo.findByMission_Id(102)).thenReturn(Optional.empty());

        billingService.createEstimatedPayment(mission);

        verify(paymentRepo, never()).save(any());
        verifyNoInteractions(distanceResolver, fareCalculator);
    }

    @Test
    void createEstimatedPayment_whenBillableDistanceMissing_shouldNotCreatePayment() {
        DispatchMission mission = missionGraph(103, "ALS", null, 7, 8);
        when(paymentRepo.findByMission_Id(103)).thenReturn(Optional.empty());
        when(distanceResolver.resolveBillableDistanceKm(mission)).thenReturn(Optional.empty());

        billingService.createEstimatedPayment(mission);

        verify(paymentRepo, never()).save(any());
        verifyNoInteractions(fareCalculator);
    }

    private DispatchMission missionGraph(Integer missionId, String serviceTypeCode,
                                         String reporterPhone, Integer providerId, Integer driverId) {
        ServiceType serviceType = new ServiceType();
        serviceType.setTypeCode(serviceTypeCode);

        EmergencyCall call = new EmergencyCall();
        call.setReporterPhone(reporterPhone);

        DispatchRequest request = new DispatchRequest();
        request.setServiceType(serviceType);
        request.setCall(call);

        Provider provider = new Provider();
        provider.setId(providerId);

        User driver = new User();
        driver.setId(driverId);

        DispatchResource resource = new DispatchResource();
        resource.setProvider(provider);
        resource.setCurrentDriver(driver);

        DispatchMission mission = new DispatchMission();
        mission.setId(missionId);
        mission.setRequest(request);
        mission.setResource(resource);
        return mission;
    }
}
