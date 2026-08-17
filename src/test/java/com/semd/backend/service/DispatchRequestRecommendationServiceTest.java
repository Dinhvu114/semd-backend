package com.semd.backend.service;

import com.semd.backend.dto.response.RecommendationItemDto;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.DispatchRequestStatus;
import com.semd.backend.entity.DispatchResource;
import com.semd.backend.entity.DispatchResourceStatus;
import com.semd.backend.entity.User;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.MissionStatusLogRepository;
import com.semd.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatchRequestRecommendationServiceTest {

    private DispatchRequestRepository requestRepository;
    private DispatchResourceRepository resourceRepository;
    private DispatchRequestService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(DispatchRequestRepository.class);
        resourceRepository = mock(DispatchResourceRepository.class);
        service = new DispatchRequestService(
                requestRepository,
                mock(DispatchMissionRepository.class),
                resourceRepository,
                mock(MissionStatusLogRepository.class),
                mock(UserRepository.class),
                mock(SimpMessagingTemplate.class),
                mock(RestTemplate.class));
    }

    @Test
    void recommendsOnlyTopThreeEligibleResourcesOrderedByScore() {
        DispatchRequest request = requestAt(15, 21.0285, 105.8542);
        DispatchResource nearest = resourceAt(1, 21.0290, 105.8550, true);
        DispatchResource second = resourceAt(2, 21.0350, 105.8600, true);
        DispatchResource third = resourceAt(3, 21.0450, 105.8700, true);
        DispatchResource fourth = resourceAt(4, 21.0600, 105.8900, true);
        DispatchResource withoutDriver = resourceAt(5, 21.0286, 105.8543, false);

        when(requestRepository.findById(15)).thenReturn(Optional.of(request));
        when(resourceRepository.findAllByStatus(DispatchResourceStatus.AVAILABLE))
                .thenReturn(List.of(fourth, withoutDriver, second, third, nearest));

        List<RecommendationItemDto> result = service.recommend(15);

        assertEquals(3, result.size());
        assertEquals(List.of(1, 2, 3),
                result.stream().map(RecommendationItemDto::resourceId).toList());
        assertEquals(List.of(1, 2, 3),
                result.stream().map(RecommendationItemDto::rank).toList());
        assertTrue(result.get(0).score() >= result.get(1).score());
        assertTrue(result.get(1).score() >= result.get(2).score());
        assertEquals("HAVERSINE_ESTIMATE", result.get(0).etaSource());
        assertNotNull(result.get(0).breakdown());
    }

    @Test
    void excludesResourceWithStaleLocation() {
        DispatchRequest request = requestAt(20, 21.0285, 105.8542);
        DispatchResource stale = resourceAt(1, 21.0290, 105.8550, true);
        stale.setUpdatedAt(LocalDateTime.now().minusMinutes(30));

        when(requestRepository.findById(20)).thenReturn(Optional.of(request));
        when(resourceRepository.findAllByStatus(DispatchResourceStatus.AVAILABLE))
                .thenReturn(List.of(stale));

        assertTrue(service.recommend(20).isEmpty());
    }

    private DispatchRequest requestAt(Integer id, double latitude, double longitude) {
        DispatchRequest request = new DispatchRequest();
        request.setId(id);
        request.setStatus(DispatchRequestStatus.CONFIRMED);
        request.setTargetLocation(point(latitude, longitude));
        return request;
    }

    private DispatchResource resourceAt(
            Integer id, double latitude, double longitude, boolean hasDriver) {
        DispatchResource resource = new DispatchResource();
        resource.setId(id);
        resource.setResourceCode("AMB-" + id);
        resource.setStatus(DispatchResourceStatus.AVAILABLE);
        resource.setCurrentLocation(point(latitude, longitude));
        resource.setUpdatedAt(LocalDateTime.now());
        if (hasDriver) {
            User driver = new User();
            driver.setId(100 + id);
            resource.setCurrentDriver(driver);
        }
        return resource;
    }

    private org.locationtech.jts.geom.Point point(double latitude, double longitude) {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        return factory.createPoint(new Coordinate(longitude, latitude));
    }
}
