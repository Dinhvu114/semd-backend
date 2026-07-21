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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

        // 3. Đặt trạng thái xe sang DISPATCHED
        resource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(resource);

        // 4. Tạo mission
        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestinationName(req.getDestinationName());
        mission.setNotes(req.getNotes());
        mission.setStatus(DispatchMissionStatus.CREATED);

        DispatchMission saved = missionRepository.save(mission);

        // 5. Cập nhật trạng thái request
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        // 6. Ghi log
        MissionStatusLog log = new MissionStatusLog();
        log.setMission(saved);
        log.setOldStatus(null);
        log.setNewStatus(DispatchMissionStatus.CREATED.name());
        log.setNote("Tạo nhiệm vụ và điều phối xe: " + resource.getResourceCode());
        log.setCreatedAt(LocalDateTime.now());
        statusLogRepository.save(log);

        // 7. Gửi WebSocket cho Dispatcher
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

        // 8. Gửi WebSocket cho Driver
        User currentDriver = resource.getCurrentDriver();

        if (currentDriver != null) {
            Integer driverId = currentDriver.getId();

            messagingTemplate.convertAndSend(
                    "/topic/driver/" + driverId,
                    (Object) Map.of(
                            "event",       "MISSION_ASSIGNED",
                            "missionId",   saved.getId(),
                            "requestId",   req.getRequestId(),
                            "destination", req.getDestinationName() != null ? req.getDestinationName() : "",
                            "urgency",     request.getUrgencyLevel(),
                            "status",      "DISPATCHED",
                            "message",     "Bạn vừa được phân công nhiệm vụ mới!"
                    )
            );
        }

        // 9. Trả về response
        return toResponse(saved);
    }

    public DispatchMissionResponse updateStatus(Integer missionId, String newStatus) {

        List<String> validStatuses = List.of("ACCEPTED", "ON_SCENE", "COMPLETED");

        if (!validStatuses.contains(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus
                    + ". Chỉ chấp nhận: " + validStatuses);
        }

        DispatchMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mission id: " + missionId));

        // Cập nhật trạng thái bằng enum
        switch (newStatus) {
            case "ACCEPTED"  -> {
                mission.setStatus(DispatchMissionStatus.ACCEPTED);
                mission.setAcceptedAt(LocalDateTime.now());
            }
            case "ON_SCENE"  -> {
                mission.setStatus(DispatchMissionStatus.ON_SCENE);
                mission.setOnSceneAt(LocalDateTime.now());
            }
            case "COMPLETED" -> {
                mission.setStatus(DispatchMissionStatus.COMPLETED);
                mission.setCompletedAt(LocalDateTime.now());
            }
        }

        DispatchMission saved = missionRepository.save(mission);

        // Thông báo Dispatcher
        messagingTemplate.convertAndSend(
                "/topic/dispatcher/missions",
                (Object) Map.of(
                        "event",     "MISSION_STATUS_UPDATED",
                        "missionId", saved.getId(),
                        "newStatus", newStatus,
                        "message",   "Driver đã cập nhật trạng thái: " + newStatus
                )
        );

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