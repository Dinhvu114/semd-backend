package com.semd.backend.service;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DispatchMissionService {

    // Status được coi là đang hoạt động
    private static final List<DispatchMissionStatus> ACTIVE_STATUSES = List.of(
            DispatchMissionStatus.DISPATCHED,
            DispatchMissionStatus.ACCEPTED,
            DispatchMissionStatus.EN_ROUTE,
            DispatchMissionStatus.ARRIVED_SCENE,
            DispatchMissionStatus.TRANSPORTING,
            DispatchMissionStatus.ARRIVED_HOSPITAL
    );

    private final DispatchMissionRepository missionRepository;
    private final DispatchRequestRepository requestRepository;
    private final DispatchResourceRepository resourceRepository;
    private final MissionStatusLogRepository statusLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MedicalHospitalRepository hospitalRepository;

    public DispatchMissionService(
            DispatchMissionRepository missionRepository,
            DispatchRequestRepository requestRepository,
            DispatchResourceRepository resourceRepository,
            MissionStatusLogRepository statusLogRepository,
            SimpMessagingTemplate messagingTemplate,
                MedicalHospitalRepository hospitalRepository) {
        this.missionRepository = missionRepository;
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
        this.statusLogRepository = statusLogRepository;
        this.messagingTemplate = messagingTemplate;
        this.hospitalRepository = hospitalRepository;

    }

    // ══════════════════════════════════════════════════════
    // TẠO MISSION
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse create(CreateDispatchMissionRequest req) {

        // 1. Lấy request + kiểm tra tồn tại
        DispatchRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new MissionException(404, "REQUEST_NOT_FOUND",
                        "Không tìm thấy dispatch_request id: " + req.getRequestId()));

        // 2. Request phải ở RECOMMENDING (đã qua bước đề xuất xe)
        if (request.getStatus() != DispatchRequestStatus.RECOMMENDING) {
            throw new MissionException(409, "REQUEST_NOT_READY_FOR_DISPATCH",
                    "Request phải ở trạng thái RECOMMENDING trước khi tạo mission. "
                            + "Trạng thái hiện tại: " + request.getStatus());
        }

        // 3. Kiểm tra request chưa có mission đang hoạt động
        if (missionRepository.findActiveMissionByRequestId(req.getRequestId()).isPresent()) {
            throw new MissionException(409, "ACTIVE_MISSION_ALREADY_EXISTS",
                    "Request đang có một mission hoạt động");
        }

        // 4. Lấy xe + LOCK tránh race condition
        DispatchResource resource = resourceRepository
                .findByIdWithLock(req.getResourceId())
                .orElseThrow(() -> new MissionException(404, "RESOURCE_NOT_FOUND",
                        "Không tìm thấy xe id: " + req.getResourceId()));

        // 5. Kiểm tra xe còn AVAILABLE
        if (resource.getStatus() != DispatchResourceStatus.AVAILABLE) {
            throw new MissionException(409, "RESOURCE_NOT_AVAILABLE",
                    "Xe đang bận, status: " + resource.getStatus());
        }

        // 6. Kiểm tra xe chưa có mission đang chạy
        if (missionRepository.existsByResourceIdAndStatusIn(resource.getId(), ACTIVE_STATUSES)) {
            throw new MissionException(409, "RESOURCE_HAS_ACTIVE_MISSION",
                    "Xe đang có nhiệm vụ chưa hoàn thành");
        }
        MedicalHospital destination = null;

        if (req.getDestinationId() != null) {
        destination = hospitalRepository
                .findById(req.getDestinationId())
                .orElseThrow(() ->
                        new MissionException(
                                404,
                                "DESTINATION_NOT_FOUND",
                                "Không tìm thấy bệnh viện id: "
                                        + req.getDestinationId()
                        )
                );
        }

        // 7. Chuyển request sang DISPATCHING (đang trong quá trình giao)
        request.setStatus(DispatchRequestStatus.DISPATCHING);
        requestRepository.save(request);

        // 8. Tạo mission DISPATCHED
        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestination(destination);

        mission.setDestinationName(
                destination != null
                        ? destination.getHospitalName()
                        : req.getDestinationName()
        );
        mission.setNotes(req.getNotes());
        mission.setStatus(DispatchMissionStatus.DISPATCHED);
        mission.setDispatchedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        // 9. Chuyển xe sang DISPATCHED (chờ driver xác nhận)
        resource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(resource);

        // 10. Chuyển request sang DISPATCHED
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        // 11. Ghi audit log
        saveLog(saved, null, DispatchMissionStatus.DISPATCHED,
                "Tạo nhiệm vụ, điều xe: " + resource.getResourceCode());

        // 12. Gửi WebSocket
        notifyDispatcher("NEW_MISSION", saved,
                "Nhiệm vụ mới được tạo cho xe " + resource.getResourceCode());

        User currentDriver = resource.getCurrentDriver();
        if (currentDriver != null) {
            notifyDriver(currentDriver.getId(), "MISSION_ASSIGNED", saved,
                    "Bạn vừa được phân công nhiệm vụ mới!");
        }

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // GET
    // ══════════════════════════════════════════════════════
    public DispatchMissionResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public List<DispatchMissionResponse> getAll() {
        return missionRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    public List<DispatchMissionResponse> getMyMissions(Integer driverId) {
        return missionRepository.findAllByDriverId(driverId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DispatchMissionResponse> getMyActiveMissions(Integer driverId) {
        return missionRepository.findByDriverIdAndStatusIn(driverId, ACTIVE_STATUSES).stream()
                .map(this::toResponse)
                .toList();
    }

    public DispatchMissionResponse getMyMission(Integer driverId, Integer missionId) {
        DispatchMission mission = missionRepository.findByIdAndDriverId(missionId, driverId)
                .orElseThrow(() -> new MissionException(404, "MISSION_NOT_FOUND",
                        "Không tìm thấy nhiệm vụ id: " + missionId));
        return toResponse(mission);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ACCEPT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse accept(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.DISPATCHED, "accept");

        // Kiểm tra và cập nhật resource
        DispatchResource resource = mission.getResource();
        if (resource.getStatus() != DispatchResourceStatus.DISPATCHED) {
            throw new MissionException(409, "INVALID_RESOURCE_STATUS",
                    "Resource phải ở trạng thái DISPATCHED trước khi nhận nhiệm vụ. "
                            + "Trạng thái hiện tại: " + resource.getStatus());
        }
        resource.setStatus(DispatchResourceStatus.ON_MISSION);
        resourceRepository.save(resource);

        mission.setStatus(DispatchMissionStatus.ACCEPTED);
        mission.setAcceptedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.DISPATCHED,
                DispatchMissionStatus.ACCEPTED, "Driver đã nhận nhiệm vụ");

        notifyDispatcher("MISSION_ACCEPTED", saved, "Driver đã xác nhận nhận nhiệm vụ");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: REJECT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse reject(Integer missionId, RejectMissionRequest req, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.DISPATCHED, "reject");

        mission.setStatus(DispatchMissionStatus.REJECTED);
        mission.setRejectReason(req.getReason());
        DispatchMission saved = missionRepository.save(mission);

        // Giải phóng xe về AVAILABLE
        DispatchResource resource = mission.getResource();
        resource.setStatus(DispatchResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

        saveLog(saved, DispatchMissionStatus.DISPATCHED,
                DispatchMissionStatus.REJECTED, "Driver từ chối: " + req.getReason());

        notifyDispatcher("MISSION_REJECTED", saved,
                "Driver từ chối nhiệm vụ. Lý do: " + req.getReason());

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: START → EN_ROUTE
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse start(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.ACCEPTED, "start");

        mission.setStatus(DispatchMissionStatus.EN_ROUTE);
        mission.setEnRouteAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.ACCEPTED,
                DispatchMissionStatus.EN_ROUTE, "Driver bắt đầu di chuyển đến hiện trường");

        notifyDispatcher("MISSION_EN_ROUTE", saved, "Xe đang trên đường đến hiện trường");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ARRIVE SCENE
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse arriveScene(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.EN_ROUTE, "arrive-scene");

        mission.setStatus(DispatchMissionStatus.ARRIVED_SCENE);
        mission.setArrivedSceneAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.EN_ROUTE,
                DispatchMissionStatus.ARRIVED_SCENE, "Xe đã đến hiện trường");

        notifyDispatcher("MISSION_ARRIVED_SCENE", saved, "Xe đã đến hiện trường");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: START TRANSPORT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse startTransport(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.ARRIVED_SCENE, "start-transport");
         if (mission.getDestination() == null) {
        throw new MissionException(
                409,
                "DESTINATION_NOT_SELECTED",
                "Chưa chọn bệnh viện đích cho nhiệm vụ"
        );
    }
        mission.setStatus(DispatchMissionStatus.TRANSPORTING);
        mission.setStartTransportAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.ARRIVED_SCENE,
                DispatchMissionStatus.TRANSPORTING, "Bắt đầu chở bệnh nhân đến bệnh viện");

        notifyDispatcher("MISSION_TRANSPORTING", saved, "Xe đang chở bệnh nhân đến bệnh viện");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ARRIVE HOSPITAL
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse arriveHospital(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.TRANSPORTING, "arrive-hospital");

        mission.setStatus(DispatchMissionStatus.ARRIVED_HOSPITAL);
        mission.setArrivedHospitalAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.TRANSPORTING,
                DispatchMissionStatus.ARRIVED_HOSPITAL, "Xe đã đến bệnh viện");

        notifyDispatcher("MISSION_ARRIVED_HOSPITAL", saved, "Xe đã đến bệnh viện");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: COMPLETE — đóng ca, giải phóng xe
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse complete(Integer missionId, Integer driverId) {
        DispatchMission mission = getOwnedMissionForUpdate(missionId, driverId);
        assertStatus(mission, DispatchMissionStatus.ARRIVED_HOSPITAL, "complete");

        mission.setStatus(DispatchMissionStatus.COMPLETED);
        mission.setCompletedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        // Giải phóng xe
        DispatchResource resource = mission.getResource();
        resource.setStatus(DispatchResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

        // Đóng request
        DispatchRequest request = mission.getRequest();
        request.setStatus(DispatchRequestStatus.COMPLETED);
        requestRepository.save(request);

        saveLog(saved, DispatchMissionStatus.ARRIVED_HOSPITAL,
                DispatchMissionStatus.COMPLETED,
                "Hoàn thành nhiệm vụ, xe đã được giải phóng");

        notifyDispatcher("MISSION_COMPLETED", saved,
                "Nhiệm vụ hoàn thành. Xe " + resource.getResourceCode() + " đã sẵn sàng");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DISPATCHER: REDISPATCH — điều xe khác khi driver từ chối. Chỉ thay nhiệm vụ cũ và giữ điểm đến
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse redispatch(Integer requestId, Integer newResourceId) {

        // 1. Lấy request
        DispatchRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new MissionException(404, "REQUEST_NOT_FOUND",
                        "Không tìm thấy request id: " + requestId));

        // 2. Chỉ redispatch khi request đang DISPATCHED
        if (request.getStatus() != DispatchRequestStatus.DISPATCHED) {
            throw new BusinessConflictException(
                    "Chỉ request ở trạng thái DISPATCHED mới được điều phối lại. "
                            + "Trạng thái hiện tại: " + request.getStatus());
        }

        // 3. Lấy mission cũ để giữ lại bệnh viện đích khi redispatch, sau đó mới hủy nhiệm vụ cũ
        DispatchMission oldMission =
                missionRepository.findActiveMissionByRequestId(requestId)
                        .orElse(null);

        MedicalHospital destination =
                oldMission != null
                        ? oldMission.getDestination()
                        : null;

        String destinationName =
                oldMission != null
                        ? oldMission.getDestinationName()
                        : null;

        // Huỷ mission cũ nếu có
        if (oldMission != null) {
        DispatchMissionStatus oldStatus = oldMission.getStatus();

        oldMission.setStatus(DispatchMissionStatus.CANCELLED);
        missionRepository.save(oldMission);

        // Giải phóng xe cũ
        DispatchResource oldResource = oldMission.getResource();
        oldResource.setStatus(DispatchResourceStatus.AVAILABLE);
        resourceRepository.save(oldResource);

        // Log
        saveLog(
                oldMission,
                oldStatus,
                DispatchMissionStatus.CANCELLED,
                "Huỷ để điều xe mới"
        );
        }

        // 4. Lấy xe mới + LOCK
        DispatchResource newResource = resourceRepository
                .findByIdWithLock(newResourceId)
                .orElseThrow(() -> new MissionException(404, "RESOURCE_NOT_FOUND",
                        "Không tìm thấy xe id: " + newResourceId));

        if (newResource.getStatus() != DispatchResourceStatus.AVAILABLE) {
            throw new MissionException(409, "RESOURCE_NOT_AVAILABLE",
                    "Xe mới đang bận, status: " + newResource.getStatus());
        }

        // 5. Tạo mission mới
        DispatchMission newMission = new DispatchMission();
        newMission.setRequest(request);
        newMission.setResource(newResource);
        newMission.setDestination(destination);
        newMission.setDestinationName(destinationName);
        newMission.setStatus(DispatchMissionStatus.DISPATCHED);
        newMission.setDispatchedAt(LocalDateTime.now()); // timestamp đầy đủ
        DispatchMission saved = missionRepository.save(newMission);

        // 6. Cập nhật xe mới
        newResource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(newResource);

        // 7. Request giữ DISPATCHED (self-transition)
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        // 8. Log
        saveLog(saved, null, DispatchMissionStatus.DISPATCHED,
                "Điều xe mới: " + newResource.getResourceCode());

        // 9. WebSocket
        notifyDispatcher("MISSION_REDISPATCHED", saved,
                "Đã điều xe mới: " + newResource.getResourceCode());

        User newDriver = newResource.getCurrentDriver();
        if (newDriver != null) {
            notifyDriver(newDriver.getId(), "MISSION_ASSIGNED", saved,
                    "Bạn vừa được phân công nhiệm vụ mới!");
        }

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // HELPER — giữ lại updateStatus cho tương thích endpoint cũ
    // ══════════════════════════════════════════════════════
//     public DispatchMissionResponse updateStatus(Integer missionId, String newStatus) {
//         return switch (newStatus) {
//             case "ACCEPTED"          -> accept(missionId, null);
//             case "EN_ROUTE"          -> start(missionId, null);
//             case "ARRIVED_SCENE"     -> arriveScene(missionId, null);
//             case "TRANSPORTING"      -> startTransport(missionId, null);
//             case "ARRIVED_HOSPITAL"  -> arriveHospital(missionId, null);
//             case "COMPLETED"         -> complete(missionId, null);
//             default -> throw new MissionException(400, "INVALID_STATUS",
//                     "Trạng thái không hợp lệ: " + newStatus);
//         };
//     }

    private DispatchMission findOrThrow(Integer id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new MissionException(404, "MISSION_NOT_FOUND",
                        "Không tìm thấy mission id: " + id));
    }

    private DispatchMission lockOrThrow(Integer id) {
        return missionRepository.findByIdWithLock(id)
                .orElseThrow(() -> new MissionException(404, "MISSION_NOT_FOUND",
                        "Không tìm thấy mission id: " + id));
    }

    private DispatchMission getOwnedMissionForUpdate(Integer missionId, Integer driverId) {
        DispatchMission mission = lockOrThrow(missionId);

        if (driverId != null) {
            if (mission.getResource() == null
                    || mission.getResource().getCurrentDriver() == null
                    || !mission.getResource()
                            .getCurrentDriver()
                            .getId()
                            .equals(driverId)) {

                throw new AccessDeniedException(
                        "Nhiệm vụ không thuộc tài xế hiện tại"
                );
            }
        }
        return mission;
    }

    private void assertStatus(DispatchMission mission,
                              DispatchMissionStatus expected,
                              String action) {
        if (mission.getStatus() != expected) {
            throw new MissionException(409, "INVALID_STATUS_TRANSITION",
                    "Không thể " + action + " khi mission đang ở trạng thái: "
                            + mission.getStatus() + ". Cần: " + expected);
        }
    }

    private void saveLog(DispatchMission mission,
                         DispatchMissionStatus oldStatus,
                         DispatchMissionStatus newStatus,
                         String note) {
        MissionStatusLog log = new MissionStatusLog();
        log.setMission(mission);
        log.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        log.setNewStatus(newStatus.name());
        log.setNote(note);
        log.setCreatedAt(LocalDateTime.now());
        statusLogRepository.save(log);
    }

    private void notifyDispatcher(String event, DispatchMission mission, String message) {
        messagingTemplate.convertAndSend(
                "/topic/dispatcher/missions",
                (Object) Map.of(
                        "event",     event,
                        "missionId", mission.getId(),
                        "requestId", mission.getRequest().getId(),
                        "status",    mission.getStatus().name(),
                        "message",   message
                )
        );
    }

    private void notifyDriver(Integer driverId, String event,
                              DispatchMission mission, String message) {
        if (driverId == null) return;
        messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId,
                (Object) Map.of(
                        "event",     event,
                        "missionId", mission.getId(),
                        "status",    mission.getStatus().name(),
                        "message",   message
                )
        );
    }

    private DispatchMissionResponse toResponse(DispatchMission m) {
        DispatchMissionResponse res = new DispatchMissionResponse();
        res.setId(m.getId());
        res.setRequestId(m.getRequest().getId());
        res.setResourceId(m.getResource().getId());
        if (m.getDestination() != null) {
                MedicalHospital destination = m.getDestination();

                res.setDestinationId(destination.getId());
                res.setDestinationName(destination.getHospitalName());
                res.setDestinationAddress(destination.getHospitalAddress());

                if (destination.getLocation() != null) {
                        res.setDestinationLatitude(
                                destination.getLocation().getY()
                        );
                        res.setDestinationLongitude(
                                destination.getLocation().getX()
                        );
                }
        } else {
                res.setDestinationName(m.getDestinationName());
        }
        res.setStatus(m.getStatus().name());
        res.setDispatchedAt(m.getDispatchedAt());
        res.setAcceptedAt(m.getAcceptedAt());
        res.setEnRouteAt(m.getEnRouteAt());
        res.setArrivedSceneAt(m.getArrivedSceneAt());
        res.setStartTransportAt(m.getStartTransportAt());
        res.setArrivedHospitalAt(m.getArrivedHospitalAt());
        res.setCompletedAt(m.getCompletedAt());
        res.setRejectReason(m.getRejectReason());
        res.setCancelledAt(m.getCancelledAt());
        res.setCancelReason(m.getCancelledReason());
        res.setNotes(m.getNotes());
        return res;
    }

    // Exception nội bộ
    public static class MissionException extends RuntimeException {
        private final int httpStatus;
        private final String errorCode;

        public MissionException(int httpStatus, String errorCode, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.errorCode = errorCode;
        }

        public int getHttpStatus() { return httpStatus; }
        public String getErrorCode() { return errorCode; }
    }
}
