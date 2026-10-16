package com.semd.backend.service;

import com.semd.backend.dto.emergencyCall.EmergencyCallResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyCallAiFallbackTest {

    @Mock EmergencyCallRepository callRepository;
    @Mock FileStorageService fileStorageService;
    @Mock AuditLogRepository auditLogRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock DispatchRequestRepository dispatchRequestRepository;
    @Mock ServiceTypeRepository serviceTypeRepository;
    @Mock DispatchMissionRepository missionRepository;
    @Mock AmbulanceSimulationRepository simulationRepository;
    @Mock AmbulanceJourneyService ambulanceJourneyService;
    @Mock ListOperations<String, String> listOperations;

    @InjectMocks EmergencyCallService service;

    @BeforeEach
    void setUp() {
        lenient().when(fileStorageService.getPublicUrl(anyString())).thenReturn("http://minio/audio.mp3");
    }

    @Test
    void createEmergencyVoiceCall_WhenAiWorkerIsActive_EnqueuesToRedisAndDoesNotCreateDummyRequest() {
        // AI worker is alive
        when(redisTemplate.hasKey("ai:worker:heartbeat")).thenReturn(Boolean.TRUE);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        when(callRepository.save(any(EmergencyCall.class))).thenAnswer(invocation -> {
            EmergencyCall call = invocation.getArgument(0);
            call.setId(100);
            return call;
        });

        EmergencyCallResponse response = service.createEmergencyVoiceCall(
                "0912345678", "Nguyen Van A", 10.762, 106.660, "calls/audio.mp3", "Dau tim"
        );

        assertNotNull(response);
        assertEquals(100, response.callId());
        assertNull(response.dispatchRequestId(), "Không được tạo DispatchRequest rỗng khi AI đang hoạt động bình thường!");

        // Verify enqueued to Redis
        verify(listOperations).leftPush(eq("emergency:ai:queue"), contains("\"call_id\":100"));
        // Verify no DispatchRequest saved
        verify(dispatchRequestRepository, never()).save(any(DispatchRequest.class));
    }

    @Test
    void createEmergencyVoiceCall_WhenAiWorkerIsOffline_ImmediatelyTriggersFallbackDispatchRequest() {
        // AI worker is offline
        when(redisTemplate.hasKey("ai:worker:heartbeat")).thenReturn(Boolean.FALSE);
        when(dispatchRequestRepository.findFirstByCallIdOrderByCreatedAtDesc(101)).thenReturn(Optional.empty());

        ServiceType bls = new ServiceType();
        bls.setTypeCode("BLS");
        when(serviceTypeRepository.findByTypeCode("BLS")).thenReturn(Optional.of(bls));

        when(callRepository.save(any(EmergencyCall.class))).thenAnswer(invocation -> {
            EmergencyCall call = invocation.getArgument(0);
            call.setId(101);
            return call;
        });

        when(dispatchRequestRepository.save(any(DispatchRequest.class))).thenAnswer(invocation -> {
            DispatchRequest req = invocation.getArgument(0);
            req.setId(201);
            return req;
        });

        EmergencyCallResponse response = service.createEmergencyVoiceCall(
                "0912345678", "Nguyen Van B", 10.762, 106.660, "calls/audio.mp3", "Kho tho"
        );

        assertNotNull(response);
        assertEquals(101, response.callId());
        assertEquals(201, response.dispatchRequestId(), "Phải lập tức tạo DispatchRequest dự phòng khi AI offline!");

        // Verify fallback DispatchRequest created with AI_OFFLINE review note
        ArgumentCaptor<DispatchRequest> reqCaptor = ArgumentCaptor.forClass(DispatchRequest.class);
        verify(dispatchRequestRepository).save(reqCaptor.capture());
        DispatchRequest createdReq = reqCaptor.getValue();
        assertEquals(DispatchRequestStatus.PENDING, createdReq.getStatus());
        assertTrue(createdReq.getReviewNote().contains("[AI_OFFLINE]"));

        // Verify Redis queue was NOT called
        verify(redisTemplate, never()).opsForList();
    }

    @Test
    void handleAICallback_WhenNoPriorDispatchRequest_CreatesNewRequestWithAiPredictions() {
        EmergencyCall call = new EmergencyCall();
        call.setId(102);
        call.setStatus(EmergencyCallStatus.RECEIVED);

        when(callRepository.findById(102)).thenReturn(Optional.of(call));
        when(callRepository.save(any(EmergencyCall.class))).thenReturn(call);
        when(dispatchRequestRepository.findFirstByCallIdOrderByCreatedAtDesc(102)).thenReturn(Optional.empty());

        ServiceType als = new ServiceType();
        als.setTypeCode("ALS");
        when(serviceTypeRepository.findByTypeCode("ALS")).thenReturn(Optional.of(als));

        when(dispatchRequestRepository.save(any(DispatchRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyCall result = service.handleAICallback(102, "Nạn nhân ngất xỉu ngừng thở", "CRITICAL", 0.95, List.of("ngừng thở", "hôn mê"));

        assertEquals(EmergencyCallStatus.AI_ANALYZED, result.getStatus());
        assertEquals("CRITICAL", result.getAiUrgencyPrediction());
        assertEquals(BigDecimal.valueOf(0.95), result.getAiConfidenceScore());

        ArgumentCaptor<DispatchRequest> reqCaptor = ArgumentCaptor.forClass(DispatchRequest.class);
        verify(dispatchRequestRepository).save(reqCaptor.capture());
        DispatchRequest saved = reqCaptor.getValue();
        assertEquals("CRITICAL", saved.getUrgencyLevel());
        assertEquals("ALS", saved.getServiceType().getTypeCode());
        assertNotNull(saved.getExtendedRequirements());
    }

    @Test
    void handleAICallback_WhenPriorFallbackDispatchRequestExists_UpdatesExistingRequestWithoutDuplicating() {
        EmergencyCall call = new EmergencyCall();
        call.setId(103);
        call.setStatus(EmergencyCallStatus.PENDING_REVIEW);

        when(callRepository.findById(103)).thenReturn(Optional.of(call));
        when(callRepository.save(any(EmergencyCall.class))).thenReturn(call);

        // Prior fallback DispatchRequest exists
        DispatchRequest existingReq = new DispatchRequest();
        existingReq.setId(301);
        existingReq.setCall(call);
        existingReq.setUrgencyLevel("MEDIUM");
        existingReq.setReviewNote("[AI_TIMEOUT] Quá thời gian phản hồi từ AI");
        when(dispatchRequestRepository.findFirstByCallIdOrderByCreatedAtDesc(103)).thenReturn(Optional.of(existingReq));

        ServiceType als = new ServiceType();
        als.setTypeCode("ALS");
        when(serviceTypeRepository.findByTypeCode("ALS")).thenReturn(Optional.of(als));

        when(dispatchRequestRepository.save(any(DispatchRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyCall result = service.handleAICallback(103, "Đau thắt ngực dữ dội", "HIGH", 0.88, List.of("đau ngực"));

        assertEquals(EmergencyCallStatus.AI_ANALYZED, result.getStatus());

        ArgumentCaptor<DispatchRequest> reqCaptor = ArgumentCaptor.forClass(DispatchRequest.class);
        verify(dispatchRequestRepository).save(reqCaptor.capture());
        DispatchRequest updated = reqCaptor.getValue();
        assertEquals(301, updated.getId(), "Phải cập nhật đúng bản ghi DispatchRequest hiện có!");
        assertEquals("HIGH", updated.getUrgencyLevel());
        assertEquals("ALS", updated.getServiceType().getTypeCode());
        assertTrue(updated.getReviewNote().contains("[AI Cập nhật sau]"));
    }
}
