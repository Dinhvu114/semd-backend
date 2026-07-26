package com.semd.backend.service;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.entity.DispatchMission;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.DispatchRequestStatus;
import com.semd.backend.entity.DispatchResource;
import com.semd.backend.entity.DispatchResourceStatus;
import com.semd.backend.exception.InvalidStateTransitionException;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.MissionStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DispatchMissionServiceTest {

    private DispatchMissionRepository missionRepository;
    private DispatchRequestRepository requestRepository;
    private DispatchResourceRepository resourceRepository;
    private MissionStatusLogRepository statusLogRepository;
    private DispatchMissionService service;

    @BeforeEach
    void setUp() {
        missionRepository = mock(DispatchMissionRepository.class);
        requestRepository = mock(DispatchRequestRepository.class);
        resourceRepository = mock(DispatchResourceRepository.class);
        statusLogRepository = mock(MissionStatusLogRepository.class);
        service = new DispatchMissionService(
                missionRepository,
                requestRepository,
                resourceRepository,
                statusLogRepository,
                mock(SimpMessagingTemplate.class)
        );
    }

    @Test
    void refusesMissionWhenRequestIsNotConfirmed() {
        DispatchRequest request = new DispatchRequest();
        request.setStatus(DispatchRequestStatus.PENDING);
        when(requestRepository.findByIdForUpdate(10)).thenReturn(Optional.of(request));

        assertThrows(InvalidStateTransitionException.class, () -> service.create(createRequest()));

        verify(resourceRepository, never()).findByIdForUpdate(any());
        verify(missionRepository, never()).save(any());
    }

    @Test
    void refusesMissionWhenResourceIsNotAvailable() {
        DispatchRequest request = new DispatchRequest();
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        DispatchResource resource = new DispatchResource();
        resource.setStatus(DispatchResourceStatus.DISPATCHED);
        when(requestRepository.findByIdForUpdate(10)).thenReturn(Optional.of(request));
        when(resourceRepository.findByIdForUpdate(20)).thenReturn(Optional.of(resource));

        assertThrows(InvalidStateTransitionException.class, () -> service.create(createRequest()));

        verify(missionRepository, never()).save(any());
    }

    @Test
    void createsMissionForConfirmedRequestAndAvailableResource() {
        DispatchRequest request = new DispatchRequest();
        request.setId(10);
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        request.setUrgencyLevel("HIGH");

        DispatchResource resource = new DispatchResource();
        resource.setId(20);
        resource.setResourceCode("AMB-20");
        resource.setStatus(DispatchResourceStatus.AVAILABLE);

        when(requestRepository.findByIdForUpdate(10)).thenReturn(Optional.of(request));
        when(resourceRepository.findByIdForUpdate(20)).thenReturn(Optional.of(resource));
        when(missionRepository.save(any(DispatchMission.class))).thenAnswer(invocation -> {
            DispatchMission mission = invocation.getArgument(0);
            mission.setId(30);
            return mission;
        });

        var result = service.create(createRequest());

        assertEquals(30, result.getId());
        assertEquals(DispatchRequestStatus.DISPATCHED, request.getStatus());
        assertEquals(DispatchResourceStatus.DISPATCHED, resource.getStatus());
        verify(requestRepository).save(request);
        verify(resourceRepository).save(resource);
        verify(statusLogRepository).save(any());
    }

    private CreateDispatchMissionRequest createRequest() {
        CreateDispatchMissionRequest request = new CreateDispatchMissionRequest();
        request.setRequestId(10);
        request.setResourceId(20);
        request.setDestinationName("Bệnh viện trung tâm");
        request.setNotes("Ca kiểm thử");
        return request;
    }
}
