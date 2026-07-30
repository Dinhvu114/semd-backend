package com.semd.backend.service;

import com.semd.backend.dto.request.CancelMissionRequest;
import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DispatchMissionService {

    // Các status được coi là "đang hoạt động" của mission
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

    // ══════════════════════════════════════════════════════
    // TẠO MISSION — transaction đầy đủ
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse create(CreateDispatchMissionRequest req) {

        // 1. Lấy request + kiểm tra tồn tại
        DispatchRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new MissionException(404, "REQUEST_NOT_FOUND",
                        "Không tìm thấy dispatch_request id: " + req.getRequestId()));

        // 2. Kiểm tra request đã được xác minh chưa
        if (request.getStatus() != DispatchRequestStatus.VERIFIED) {
            throw new MissionException(409, "REQUEST_NOT_VERIFIED",
                    "Request chưa được Dispatcher xác minh, status hiện tại: "
                            + request.getStatus());
        }

        // 3. Kiểm tra request chưa có mission đang hoạt động
        if (missionRepository.existsByRequestId(req.getRequestId())) {
            throw new MissionException(409, "MISSION_ALREADY_EXISTS",
                    "Request này đã có mission, không tạo thêm được");
        }

        // 4. Lấy xe + LOCK để tránh race condition
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
        if (missionRepository.existsByResourceIdAndStatusIn(
                resource.getId(), ACTIVE_STATUSES)) {
            throw new MissionException(409, "RESOURCE_HAS_ACTIVE_MISSION",
                    "Xe đang có nhiệm vụ chưa hoàn thành");
        }

        // 7. Tạo mission DISPATCHED
        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestinationName(req.getDestinationName());
        mission.setNotes(req.getNotes());
        mission.setStatus(DispatchMissionStatus.DISPATCHED);
        mission.setDispatchedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        // 8. Chuyển xe sang BUSY
        resource.setStatus(DispatchResourceStatus.ON_MISSION);
        resourceRepository.save(resource);

        // 9. Chuyển request sang DISPATCHED
        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        // 10. Ghi audit log
        saveLog(saved, null, DispatchMissionStatus.DISPATCHED,
                "Tạo nhiệm vụ, điều xe: " + resource.getResourceCode());

        // 11. Gửi WebSocket SAU KHI transaction thành công
        notifyDispatcher("NEW_MISSION", saved,
                "Nhiệm vụ mới được tạo cho xe " + resource.getResourceCode());
        Integer driverId = resource.getCurrentDriver() != null
                ? resource.getCurrentDriver().getId()
                : null;
        notifyDriver(driverId, "MISSION_ASSIGNED", saved,
                "Bạn vừa được phân công nhiệm vụ mới!");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // GET — Xem chi tiết và danh sách
    // ══════════════════════════════════════════════════════
    public DispatchMissionResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public List<DispatchMissionResponse> getAll() {
        return missionRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ACCEPT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse accept(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

        assertStatus(mission, DispatchMissionStatus.DISPATCHED, "accept");

        mission.setStatus(DispatchMissionStatus.ACCEPTED);
        mission.setAcceptedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.DISPATCHED,
                DispatchMissionStatus.ACCEPTED, "Driver đã nhận nhiệm vụ");

        notifyDispatcher("MISSION_ACCEPTED", saved,
                "Driver đã xác nhận nhận nhiệm vụ");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: REJECT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse reject(Integer missionId, RejectMissionRequest req) {
        DispatchMission mission = lockOrThrow(missionId);

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
    // DRIVER: START (bắt đầu di chuyển)
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse start(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

        assertStatus(mission, DispatchMissionStatus.ACCEPTED, "start");

        mission.setStatus(DispatchMissionStatus.EN_ROUTE);
        mission.setEnRouteAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.ACCEPTED,
                DispatchMissionStatus.EN_ROUTE, "Driver bắt đầu di chuyển đến hiện trường");

        notifyDispatcher("MISSION_EN_ROUTE", saved,
                "Xe đang trên đường đến hiện trường");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ARRIVE SCENE
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse arriveScene(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

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
    // DRIVER: START TRANSPORT (bắt đầu chở bệnh nhân)
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse startTransport(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

        assertStatus(mission, DispatchMissionStatus.ARRIVED_SCENE, "start-transport");

        mission.setStatus(DispatchMissionStatus.TRANSPORTING);
        mission.setStartTransportAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.ARRIVED_SCENE,
                DispatchMissionStatus.TRANSPORTING, "Bắt đầu chở bệnh nhân đến bệnh viện");

        notifyDispatcher("MISSION_TRANSPORTING", saved,
                "Xe đang chở bệnh nhân đến bệnh viện");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ARRIVE HOSPITAL
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse arriveHospital(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

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
    public DispatchMissionResponse complete(Integer missionId) {
        DispatchMission mission = lockOrThrow(missionId);

        assertStatus(mission, DispatchMissionStatus.ARRIVED_HOSPITAL, "complete");

        mission.setStatus(DispatchMissionStatus.COMPLETED);
        mission.setCompletedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        // Giải phóng xe về AVAILABLE
        DispatchResource resource = mission.getResource();
        resource.setStatus(DispatchResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

        // Đóng request
        mission.getRequest().setStatus(DispatchRequestStatus.COMPLETED);
        requestRepository.save(mission.getRequest());

        saveLog(saved, DispatchMissionStatus.ARRIVED_HOSPITAL,
                DispatchMissionStatus.COMPLETED, "Hoàn thành nhiệm vụ, xe đã được giải phóng");

        notifyDispatcher("MISSION_COMPLETED", saved,
                "Nhiệm vụ hoàn thành. Xe " + resource.getResourceCode() + " đã sẵn sàng");

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // HELPER METHODS
    // ══════════════════════════════════════════════════════
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
        res.setDestinationName(m.getDestinationName());
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

    // Exception nội bộ để trả đúng HTTP status
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

    // Giữ lại để tương thích với endpoint cũ
    public DispatchMissionResponse updateStatus(Integer missionId, String newStatus) {
        return switch (newStatus) {
            case "ACCEPTED"         -> accept(missionId);
            case "EN_ROUTE"         -> start(missionId);
            case "ARRIVED_SCENE"    -> arriveScene(missionId);
            case "TRANSPORTING"     -> startTransport(missionId);
            case "ARRIVED_HOSPITAL" -> arriveHospital(missionId);
            case "COMPLETED"        -> complete(missionId);
            default -> throw new MissionException(400, "INVALID_STATUS",
                    "Trạng thái không hợp lệ: " + newStatus);
        };
    }
}