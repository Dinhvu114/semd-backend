package com.semd.backend.service;

import com.semd.backend.dto.response.DriverEarningDetailResponse;
import com.semd.backend.dto.response.DriverEarningSummaryResponse;
import com.semd.backend.dto.response.PaymentDetailResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @Mock PaymentTransactionRepository paymentRepo;
    @InjectMocks PaymentQueryService service;

    @Test
    void getMyPaymentByCallId_whenOwned_shouldReturnMappedPayment() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        when(paymentRepo.findByMission_Request_Call_Id(10)).thenReturn(Optional.of(payment));

        PaymentDetailResponse result = service.getMyPaymentByCallId(40, 10);

        assertEquals(1L, result.getPaymentId());
        assertEquals(10, result.getCallId());
        assertEquals(20, result.getRequestId());
        assertEquals(30, result.getMissionId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(0, result.getTotalAmount().compareTo(new BigDecimal("600000")));
    }

    @Test
    void getMyPaymentByCallId_whenOtherPayer_shouldHideTransaction() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 41, "PENDING");
        when(paymentRepo.findByMission_Request_Call_Id(10)).thenReturn(Optional.of(payment));

        assertThrows(ResourceNotFoundException.class,
                () -> service.getMyPaymentByCallId(40, 10));
    }

    @Test
    void payPayment_vietqrPending_shouldMarkSuccessAndPersist() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));

        PaymentDetailResponse result = service.payPayment(40, 1L, PaymentMethod.VIETQR);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("VIETQR", result.getPaymentMethod());
        assertNotNull(result.getPaidAt());
        assertNotNull(result.getExternalTransactionId());
        assertTrue(result.getExternalTransactionId().startsWith("TXN-"));
        verify(paymentRepo).save(payment);
    }

    @Test
    void payPayment_cashFromReporter_shouldRejectAndNotPersist() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BusinessConflictException.class,
                () -> service.payPayment(40, 1L, PaymentMethod.CASH));

        verify(paymentRepo, never()).save(any());
        assertEquals("PENDING", payment.getStatus());
    }

    @Test
    void payPayment_whenAlreadySuccess_shouldRejectDuplicatePayment() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "SUCCESS");
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(BusinessConflictException.class,
                () -> service.payPayment(40, 1L, PaymentMethod.MOMO));

        verify(paymentRepo, never()).save(any());
    }

    @Test
    void collectCash_whenOwnedAndPending_shouldMarkSuccess() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        User driver = new User();
        driver.setId(50);
        payment.setDriver(driver);
        when(paymentRepo.findByMission_Id(30)).thenReturn(Optional.of(payment));

        DriverEarningDetailResponse result = service.collectCash(50, 30);

        assertEquals("SUCCESS", result.getPaymentStatus());
        assertEquals("CASH", result.getPaymentMethod());
        assertNotNull(result.getPaidAt());
        verify(paymentRepo).save(payment);
    }

    @Test
    void collectCash_whenOtherDriver_shouldReject() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        User driver = new User();
        driver.setId(51);
        payment.setDriver(driver);
        when(paymentRepo.findByMission_Id(30)).thenReturn(Optional.of(payment));

        assertThrows(ResourceNotFoundException.class,
                () -> service.collectCash(50, 30));

        verify(paymentRepo, never()).save(any());
    }

    @Test
    void getMyEarningSummary_shouldSeparatePendingAndPaid() {
        PaymentTransaction pending = payment(1L, 10, 20, 30, 40, "PENDING");
        pending.setDriverAmount(new BigDecimal("108000"));
        PaymentTransaction paid = payment(2L, 11, 21, 31, 40, "SUCCESS");
        paid.setDriverAmount(new BigDecimal("180000"));
        PaymentTransaction ignoredStatus = payment(3L, 12, 22, 32, 40, "FAILED");
        ignoredStatus.setDriverAmount(new BigDecimal("999999"));
        when(paymentRepo.findByDriver_IdOrderByCreatedAtDesc(50))
                .thenReturn(List.of(pending, paid, ignoredStatus));

        DriverEarningSummaryResponse result = service.getMyEarningSummary(50);

        assertEquals(0, result.getPendingEarnings().compareTo(new BigDecimal("108000")));
        assertEquals(0, result.getPaidEarnings().compareTo(new BigDecimal("180000")));
        assertEquals(3, result.getMissionCount());
        // Current implementation averages pending+success over ALL payment rows, including FAILED in count.
        assertEquals(0, result.getAveragePerMission().compareTo(new BigDecimal("96000")));
    }

    @Test
    void getPaymentByRequestId_shouldUseRequestRepositoryPath() {
        PaymentTransaction payment = payment(1L, 10, 20, 30, 40, "PENDING");
        when(paymentRepo.findByMission_Request_Id(20)).thenReturn(Optional.of(payment));

        PaymentDetailResponse result = service.getPaymentByRequestId(20);

        assertEquals(20, result.getRequestId());
        assertEquals(30, result.getMissionId());
        verify(paymentRepo).findByMission_Request_Id(20);
    }

    private PaymentTransaction payment(Long paymentId, Integer callId, Integer requestId,
                                       Integer missionId, Integer payerId, String status) {
        EmergencyCall call = new EmergencyCall();
        call.setId(callId);
        call.setReporterName("Reporter");
        call.setReporterPhone("0901000001");

        DispatchRequest request = new DispatchRequest();
        request.setId(requestId);
        request.setCall(call);
        request.setConfirmedAddress("Scene");

        DispatchMission mission = new DispatchMission();
        mission.setId(missionId);
        mission.setRequest(request);
        mission.setDestinationName("Hospital");

        User payer = new User();
        payer.setId(payerId);

        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(paymentId);
        payment.setMission(mission);
        payment.setPayer(payer);
        payment.setStatus(status);
        payment.setServiceTypeCode("BLS");
        payment.setBillableDistanceKm(new BigDecimal("10.00"));
        payment.setBaseFare(new BigDecimal("200000"));
        payment.setPricePerKm(new BigDecimal("40000"));
        payment.setDistanceFare(new BigDecimal("400000"));
        payment.setAmount(new BigDecimal("600000"));
        payment.setCommissionAmount(new BigDecimal("60000"));
        payment.setDriverAmount(new BigDecimal("108000"));
        payment.setProviderAmount(new BigDecimal("432000"));
        return payment;
    }
}
