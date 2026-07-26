package com.semd.backend.service;

import com.semd.backend.dto.request.ConfirmDispatchRequest;
import com.semd.backend.dto.request.RejectDispatchRequest;
import com.semd.backend.dto.request.SeverityUpdateRequest;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.DispatchRequestStatus;
import com.semd.backend.entity.User;
import com.semd.backend.exception.InvalidStateTransitionException;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.MissionStatusLogRepository;
import com.semd.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DispatchRequestServiceTest {

    private DispatchRequestRepository requestRepository;
    private UserRepository userRepository;
    private DispatchRequestService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(DispatchRequestRepository.class);
        userRepository = mock(UserRepository.class);
        service = new DispatchRequestService(
                requestRepository,
                mock(DispatchMissionRepository.class),
                mock(DispatchResourceRepository.class),
                mock(MissionStatusLogRepository.class),
                userRepository,
                mock(SimpMessagingTemplate.class),
                mock(RestTemplate.class)
        );
    }

    @Test
    void confirmsPendingRequestWithAuthenticatedDispatcher() {
        DispatchRequest request = pendingRequest();
        User dispatcher = user(7);
        when(requestRepository.findByIdForUpdate(15)).thenReturn(Optional.of(request));
        when(userRepository.findById(7)).thenReturn(Optional.of(dispatcher));

        var result = service.confirm(15, new ConfirmDispatchRequest("Đã gọi lại xác nhận"), 7);

        assertEquals("CONFIRMED", result.get("status"));
        assertEquals(DispatchRequestStatus.CONFIRMED, request.getStatus());
        assertSame(dispatcher, request.getConfirmedBy());
        assertNotNull(request.getConfirmedAt());
        assertEquals("Đã gọi lại xác nhận", request.getReviewNote());
        verify(requestRepository).save(request);
    }

    @Test
    void rejectsPendingRequestAndStoresReviewMetadata() {
        DispatchRequest request = pendingRequest();
        User dispatcher = user(8);
        when(requestRepository.findByIdForUpdate(16)).thenReturn(Optional.of(request));
        when(userRepository.findById(8)).thenReturn(Optional.of(dispatcher));

        var result = service.reject(16, new RejectDispatchRequest("Người báo bấm nhầm SOS"), 8);

        assertEquals("REJECTED", result.get("status"));
        assertEquals(DispatchRequestStatus.REJECTED, request.getStatus());
        assertSame(dispatcher, request.getConfirmedBy());
        assertNotNull(request.getConfirmedAt());
        assertEquals("Người báo bấm nhầm SOS", request.getReviewNote());
        verify(requestRepository).save(request);
    }

    @Test
    void refusesToReviewRequestThatIsNotPending() {
        DispatchRequest request = new DispatchRequest();
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        when(requestRepository.findByIdForUpdate(15)).thenReturn(Optional.of(request));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> service.reject(15, new RejectDispatchRequest("Không hợp lệ"), 7)
        );

        verify(userRepository, never()).findById(any());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void refusesSeverityUpdateAfterDispatch() {
        DispatchRequest request = new DispatchRequest();
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        when(requestRepository.findByIdForUpdate(15)).thenReturn(Optional.of(request));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> service.updateSeverity(15, new SeverityUpdateRequest("HIGH"))
        );

        verify(requestRepository, never()).save(any());
    }

    private DispatchRequest pendingRequest() {
        DispatchRequest request = new DispatchRequest();
        request.setId(15);
        request.setStatus(DispatchRequestStatus.PENDING);
        return request;
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
