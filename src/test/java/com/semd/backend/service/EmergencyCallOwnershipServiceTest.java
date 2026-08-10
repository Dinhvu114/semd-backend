package com.semd.backend.service;

import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyCallOwnershipServiceTest {
    @Mock EmergencyCallRepository callRepository;
    @Mock FileStorageService fileStorageService;
    @Mock AuditLogRepository auditLogRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock DispatchRequestRepository dispatchRequestRepository;
    @Mock ServiceTypeRepository serviceTypeRepository;
    @Mock OperationZoneRepository operationZoneRepository;
    @Mock DispatchMissionRepository missionRepository;
    @Mock AmbulanceSimulationRepository simulationRepository;
    @Mock AmbulanceJourneyService ambulanceJourneyService;
    @InjectMocks EmergencyCallService service;

    @Test
    void getCallDetailsDoesNotFallBackToIdOnlyWhenCallerIsNotOwner() {
        when(callRepository.findByIdAndReporterPhone(99, "0900000001"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getOwnedCallDetails(99, "0900000001"));

        verify(callRepository).findByIdAndReporterPhone(99, "0900000001");
    }
}
