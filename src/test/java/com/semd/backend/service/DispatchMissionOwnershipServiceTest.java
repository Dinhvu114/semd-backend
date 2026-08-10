package com.semd.backend.service;

import com.semd.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchMissionOwnershipServiceTest {
    @Mock DispatchMissionRepository missionRepository;
    @Mock DispatchRequestRepository requestRepository;
    @Mock DispatchResourceRepository resourceRepository;
    @Mock MissionStatusLogRepository statusLogRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks DispatchMissionService service;

    @Test
    void getMyMissionUsesBothMissionIdAndAuthenticatedDriverId() {
        when(missionRepository.findByIdAndDriverId(44, 7)).thenReturn(Optional.empty());

        DispatchMissionService.MissionException error = assertThrows(
                DispatchMissionService.MissionException.class,
                () -> service.getMyMission(7, 44));

        assertEquals(404, error.getHttpStatus());
        verify(missionRepository).findByIdAndDriverId(44, 7);
    }
}
