package com.semd.backend.service;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.MissionStatusLogRepository;
import com.semd.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DispatchRequestListMappingServiceTest {

    @Test
    void includesReporterPhoneFromEmergencyCallInListResponse() {
        DispatchRequestRepository requestRepository = mock(DispatchRequestRepository.class);
        DispatchRequestService service = new DispatchRequestService(
                requestRepository,
                mock(DispatchMissionRepository.class),
                mock(DispatchResourceRepository.class),
                mock(MissionStatusLogRepository.class),
                mock(UserRepository.class),
                mock(SimpMessagingTemplate.class),
                mock(RestTemplate.class));

        EmergencyCall call = new EmergencyCall();
        call.setId(106);
        call.setReporterPhone("+84901234567");

        DispatchRequest request = new DispatchRequest();
        request.setId(24);
        request.setCall(call);

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(request), pageable, 1));

        DispatchRequestDto result = service.search(null, null, null, pageable).getContent().getFirst();

        assertEquals(106, result.callId());
        assertEquals("+84901234567", result.reporterPhone());
    }
}
