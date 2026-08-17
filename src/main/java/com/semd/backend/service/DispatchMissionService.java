package com.semd.backend.service;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.response.ActiveMissionResponse;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DispatchMissionService {

    private static final Logger log = LoggerFactory.getLogger(DispatchMissionService.class);

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
    private final AmbulanceSimulationRepository simulationRepository; // ← THÊM

    public DispatchMissionService(
            DispatchMissionRepository missionRepository,
            DispatchRequestRepository requestRepository,
            DispatchResourceRepository resourceRepository,
            MissionStatusLogRepository statusLogRepository,
            SimpMessagingTemplate messagingTemplate,
            AmbulanceSimulationRepository simulationRepository) { // ← THÊM
        this.missionRepository = missionRepository;
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
        this.statusLogRepository = statusLogRepository;
        this.messagingTemplate = messagingTemplate;
        this.simulationRepository = simulationRepository; // ← THÊM
    }

    // ══════════════════════════════════════════════════════
    // TẠO MISSION
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse create(CreateDispatchMissionRequest req) {

        DispatchRequest request = requestRepository.findById(req.getRequestId())
                .orElseThrow(() -> new MissionException(404, "REQUEST_NOT_FOUND",
                        "Không tìm thấy dispatch_request id: " + req.getRequestId()));

        if (request.getStatus() != DispatchRequestStatus.RECOMMENDING) {
            throw new MissionException(409, "REQUEST_NOT_READY_FOR_DISPATCH",
                    "Request phải ở trạng thái RECOMMENDING trước khi tạo mission. "
                            + "Trạng thái hiện tại: " + request.getStatus());
        }

        if (missionRepository.findActiveMissionByRequestId(req.getRequestId()).isPresent()) {
            throw new MissionException(409, "ACTIVE_MISSION_ALREADY_EXISTS",
                    "Request đang có một mission hoạt động");
        }

        DispatchResource resource = resourceRepository
                .findByIdWithLock(req.getResourceId())
                .orElseThrow(() -> new MissionException(404, "RESOURCE_NOT_FOUND",
                        "Không tìm thấy xe id: " + req.getResourceId()));

        if (resource.getStatus() != DispatchResourceStatus.AVAILABLE) {
            throw new MissionException(409, "RESOURCE_NOT_AVAILABLE",
                    "Xe đang bận, status: " + resource.getStatus());
        }

        if (missionRepository.existsByResourceIdAndStatusIn(resource.getId(), ACTIVE_STATUSES)) {
            throw new MissionException(409, "RESOURCE_HAS_ACTIVE_MISSION",
                    "Xe đang có nhiệm vụ chưa hoàn thành");
        }

        request.setStatus(DispatchRequestStatus.DISPATCHING);
        requestRepository.save(request);

        DispatchMission mission = new DispatchMission();
        mission.setRequest(request);
        mission.setResource(resource);
        mission.setDestinationName(req.getDestinationName());
        mission.setNotes(req.getNotes());
        mission.setStatus(DispatchMissionStatus.DISPATCHED);
        mission.setDispatchedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        resource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(resource);

        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        saveLog(saved, null, DispatchMissionStatus.DISPATCHED,
                "Tạo nhiệm vụ, điều xe: " + resource.getResourceCode());

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

    // ── Driver: lấy mission đang active ──────────────────
    public List<ActiveMissionResponse> getMyActiveMissions(Integer userId) {
        return missionRepository.findActiveByDriverId(userId)
                .stream().map(this::toActiveMissionResponse).toList();
    }

    // ── Driver: lịch sử mission ───────────────────────────
    public List<ActiveMissionResponse> getMyMissionHistory(Integer userId) {
        return missionRepository.findHistoryByDriverId(userId)
                .stream().map(this::toActiveMissionResponse).toList();
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ACCEPT
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse accept(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
        assertStatus(mission, DispatchMissionStatus.DISPATCHED, "accept");

        DispatchResource resource = mission.getResource();
        if (resource.getStatus() != DispatchResourceStatus.DISPATCHED) {
            throw new MissionException(409, "INVALID_RESOURCE_STATUS",
                    "Resource phải ở trạng thái DISPATCHED. "
                            + "Hiện tại: " + resource.getStatus());
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
    public DispatchMissionResponse reject(Integer missionId, RejectMissionRequest req,
                                          Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
        assertStatus(mission, DispatchMissionStatus.DISPATCHED, "reject");

        mission.setStatus(DispatchMissionStatus.REJECTED);
        mission.setRejectReason(req.getReason());
        DispatchMission saved = missionRepository.save(mission);

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
    // DRIVER: START → EN_ROUTE + tự động start simulation
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse start(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
        assertStatus(mission, DispatchMissionStatus.ACCEPTED, "start");

        mission.setStatus(DispatchMissionStatus.EN_ROUTE);
        mission.setEnRouteAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        saveLog(saved, DispatchMissionStatus.ACCEPTED,
                DispatchMissionStatus.EN_ROUTE, "Driver bắt đầu di chuyển đến hiện trường");

        notifyDispatcher("MISSION_EN_ROUTE", saved, "Xe đang trên đường đến hiện trường");

        // ── Tự động start simulation nếu có ─────────────
        simulationRepository.findByMissionId(missionId).ifPresent(sim -> {
            if (sim.getStatus() == SimulationStatus.READY
                    || sim.getStatus() == SimulationStatus.STOPPED) {
                if (sim.getStatus() == SimulationStatus.READY) {
                    sim.setRouteIndex(0);
                    sim.setElapsedRouteMs(0L);
                    sim.setStartedAt(java.time.OffsetDateTime.now());
                }
                sim.setStatus(SimulationStatus.RUNNING);
                simulationRepository.save(sim);
                log.info("Auto-started simulation {} khi mission {} EN_ROUTE",
                        sim.getId(), missionId);
            }
        });

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // DRIVER: ARRIVE SCENE
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse arriveScene(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
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
    public DispatchMissionResponse startTransport(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
        assertStatus(mission, DispatchMissionStatus.ARRIVED_SCENE, "start-transport");

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
    public DispatchMissionResponse arriveHospital(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
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
    // DRIVER: COMPLETE
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse complete(Integer missionId, Integer userId) {
        DispatchMission mission = lockOrThrow(missionId);
        assertDriverOwnsMission(mission, userId);
        assertStatus(mission, DispatchMissionStatus.ARRIVED_HOSPITAL, "complete");

        mission.setStatus(DispatchMissionStatus.COMPLETED);
        mission.setCompletedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(mission);

        DispatchResource resource = mission.getResource();
        resource.setStatus(DispatchResourceStatus.AVAILABLE);
        resourceRepository.save(resource);

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
    // DISPATCHER: REDISPATCH
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchMissionResponse redispatch(Integer requestId, Integer newResourceId) {

        DispatchRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new MissionException(404, "REQUEST_NOT_FOUND",
                        "Không tìm thấy request id: " + requestId));

        if (request.getStatus() != DispatchRequestStatus.DISPATCHED) {
            throw new BusinessConflictException(
                    "Chỉ request ở trạng thái DISPATCHED mới được điều phối lại. "
                            + "Trạng thái hiện tại: " + request.getStatus());
        }

        missionRepository.findActiveMissionByRequestId(requestId).ifPresent(oldMission -> {
            DispatchMissionStatus oldStatus = oldMission.getStatus();
            oldMission.setStatus(DispatchMissionStatus.CANCELLED);
            missionRepository.save(oldMission);

            DispatchResource oldResource = oldMission.getResource();
            oldResource.setStatus(DispatchResourceStatus.AVAILABLE);
            resourceRepository.save(oldResource);

            saveLog(oldMission, oldStatus, DispatchMissionStatus.CANCELLED, "Huỷ để điều xe mới");
        });

        DispatchResource newResource = resourceRepository
                .findByIdWithLock(newResourceId)
                .orElseThrow(() -> new MissionException(404, "RESOURCE_NOT_FOUND",
                        "Không tìm thấy xe id: " + newResourceId));

        if (newResource.getStatus() != DispatchResourceStatus.AVAILABLE) {
            throw new MissionException(409, "RESOURCE_NOT_AVAILABLE",
                    "Xe mới đang bận, status: " + newResource.getStatus());
        }

        DispatchMission newMission = new DispatchMission();
        newMission.setRequest(request);
        newMission.setResource(newResource);
        newMission.setStatus(DispatchMissionStatus.DISPATCHED);
        newMission.setDispatchedAt(LocalDateTime.now());
        DispatchMission saved = missionRepository.save(newMission);

        newResource.setStatus(DispatchResourceStatus.DISPATCHED);
        resourceRepository.save(newResource);

        request.setStatus(DispatchRequestStatus.DISPATCHED);
        requestRepository.save(request);

        saveLog(saved, null, DispatchMissionStatus.DISPATCHED,
                "Điều xe mới: " + newResource.getResourceCode());

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
    // HELPER
    // ══════════════════════════════════════════════════════
    private void assertDriverOwnsMission(DispatchMission mission, Integer currentUserId) {
        User assignedDriver = mission.getResource().getCurrentDriver();
        if (assignedDriver == null || !assignedDriver.getId().equals(currentUserId)) {
            throw new MissionException(403, "FORBIDDEN",
                    "Mission không thuộc tài xế hiện tại");
        }
    }

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

    private ActiveMissionResponse toActiveMissionResponse(DispatchMission m) {
        ActiveMissionResponse res = new ActiveMissionResponse();
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
        res.setNotes(m.getNotes());
        res.setUrgencyLevel(m.getRequest().getUrgencyLevel());
        simulationRepository.findByMissionId(m.getId())
                .ifPresent(sim -> res.setSimulationId(sim.getId()));
        return res;
    }

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