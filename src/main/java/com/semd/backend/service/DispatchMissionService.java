package com.semd.backend.service;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.entity.DispatchMission;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.DispatchResource;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DispatchMissionService {

    private final DispatchMissionRepository missionRepository;
    private final DispatchRequestRepository requestRepository;
    private final DispatchResourceRepository resourceRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket

    public DispatchMissionService(
            DispatchMissionRepository missionRepository,
            DispatchRequestRepository requestRepository,
            DispatchResourceRepository resourceRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.missionRepository = missionRepository;
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public DispatchMissionResponse create(CreateDispatchMissionRequest req) {

        // 1. Kiểm tra request tồn tại
        DispatchRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dispatch_request id: " + req.getRequestId()));

        // 2. Kiểm tra xe tồn tại
        DispatchResource resource = resourceRepository.findById(req.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe id: " + req.getResourceId()));

        // 3. Tạo mission
        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestinationName(req.getDestinationName());
        mission.setNotes(req.getNotes());
        mission.setStatus("DISPATCHED");

        DispatchMission saved = missionRepository.save(mission);

        // 4. Gửi thông báo WebSocket cho Dispatcher
        messagingTemplate.convertAndSend(
                "/topic/dispatcher/missions",
                (Object) Map.of(
                        "event",       "NEW_MISSION",
                        "missionId",   saved.getId(),
                        "requestId",   req.getRequestId(),
                        "resourceId",  req.getResourceId(),
                        "destination", req.getDestinationName() != null ? req.getDestinationName() : "",
                        "status",      "DISPATCHED"
                )
        );

        // 5. Trả về response
        return toResponse(saved);
    }

    private DispatchMissionResponse toResponse(DispatchMission m) {
        DispatchMissionResponse res = new DispatchMissionResponse();
        res.setId(m.getId());
        res.setRequestId(m.getRequest().getId());
        res.setResourceId(m.getResource().getId());
        res.setDestinationName(m.getDestinationName());
        res.setStatus(m.getStatus());
        res.setDispatchedAt(m.getDispatchedAt());
        res.setNotes(m.getNotes());
        return res;
    }
}