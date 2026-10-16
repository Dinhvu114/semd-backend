package com.semd.backend.scheduler;

import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.entity.EmergencyCallStatus;
import com.semd.backend.entity.EmergencyCallType;
import com.semd.backend.repository.EmergencyCallRepository;
import com.semd.backend.service.EmergencyCallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTriageTimeoutSchedulerTest {

    @Mock
    private EmergencyCallRepository callRepository;

    @Mock
    private EmergencyCallService callService;

    @InjectMocks
    private AiTriageTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "timeoutSeconds", 15);
    }

    @Test
    void scanAndHandleTimedOutCalls_WhenTimedOutCallsExist_TriggersHandleAiTimeoutForEachCall() {
        EmergencyCall call1 = new EmergencyCall();
        call1.setId(10);
        call1.setStatus(EmergencyCallStatus.RECEIVED);

        EmergencyCall call2 = new EmergencyCall();
        call2.setId(20);
        call2.setStatus(EmergencyCallStatus.RECEIVED);

        when(callRepository.findByCallTypeAndStatusAndCallStartTimeBefore(
                eq(EmergencyCallType.CALL),
                eq(EmergencyCallStatus.RECEIVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(call1, call2));

        scheduler.scanAndHandleTimedOutCalls();

        verify(callService).handleAiTimeout(10);
        verify(callService).handleAiTimeout(20);
    }

    @Test
    void scanAndHandleTimedOutCalls_WhenNoTimedOutCalls_DoesNotTriggerHandleAiTimeout() {
        when(callRepository.findByCallTypeAndStatusAndCallStartTimeBefore(
                eq(EmergencyCallType.CALL),
                eq(EmergencyCallStatus.RECEIVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        scheduler.scanAndHandleTimedOutCalls();

        verify(callService, never()).handleAiTimeout(anyInt());
    }
}
