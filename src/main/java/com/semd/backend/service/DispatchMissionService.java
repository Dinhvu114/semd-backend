package com.semd.backend.service;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.MissionStatusLogRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.LocalDateTime;

@Service
public class DispatchMissionService {

    private final DispatchMissionRepository missionRepository;
    private final DispatchRequestRepository requestRepository;
    private final DispatchResourceRepository resourceRepository;
    private final MissionStatusLogRepository statusLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DispatchMissionService(
            DispatchMissionRepository missionRepository,
            DispatchRequestRepository requestRepository,
            DispatchResourceRepository resourceRepository,
            MissionStatusLogRepository statusLogRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.missionRepository = missionRepository;
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
        this.statusLogRepository = statusLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public DispatchMissionResponse create(CreateDispatchMissionRequest req) {

        // 1. Kiểm tra request tồn tại
        DispatchRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dispatch_request id: " + req.getRequestId()));

        // 2. Kiểm tra xe tồn tại
        DispatchResource resource = resourceRepository.findById(req.getResourceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe id: " + req.getResourceId()));

        // 3. Đặt trạng thái xe cứu thương sang DISPATCHED (reserve resource)
        resource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(resource);

        // 4. Tạo mission với status CREATED
        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestinationName(req.getDestinationName());
        mission.setNotes(req.getNotes());
        mission.setStatus(DispatchMissionStatus.CREATED);

        DispatchMission saved = missionRepository.save(mission);

        // 5. Đặt trạng thái của dispatch_requests sang DISPATCHED (hoệc DISPATCHING)
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        // 6. Ghi log chuyển trạng thái (mission status transition)
        MissionStatusLog log = new MissionStatusLog();
        log.setMission(saved);
        log.setOldStatus(null);
        log.setNewStatus(DispatchMissionStatus.CREATED.name());
        log.setNote("Tạo nhiệm vụ và điều phối xe cứu thương: " + resource.getResourceCode());
        log.setCreatedAt(LocalDateTime.now());
        statusLogRepository.save(log);

        // 7. Gửi thông báo WebSocket cho Dispatcher
        messagingTemplate.convertAndSend(
                "/topic/dispatcher/missions",
                (Object) Map.of(
                        "event",       "NEW_MISSION",
                        "missionId",   saved.getId(),
                        "requestId",   req.getRequestId(),
                        "resourceId",  req.getResourceId(),
                        "destination", req.getDestinationName() != null ? req.getDestinationName() : "",
                        "status",      saved.getStatus().name()
                )
        );

        // 8. Trả về response
        return toResponse(saved);
    }

    private DispatchMissionResponse toResponse(DispatchMission m) {
        DispatchMissionResponse res = new DispatchMissionResponse();
        res.setId(m.getId());
        res.setRequestId(m.getRequest().getId());
        res.setResourceId(m.getResource().getId());
        res.setDestinationName(m.getDestinationName());
        res.setStatus(m.getStatus().name());
        res.setDispatchedAt(m.getDispatchedAt());
        res.setNotes(m.getNotes());
        return res;
    }
}