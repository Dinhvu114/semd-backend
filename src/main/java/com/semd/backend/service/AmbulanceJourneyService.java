package com.semd.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semd.backend.client.osrm.OsrmClient;
import com.semd.backend.client.osrm.OsrmRouteResponse;
import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.*;
import com.semd.backend.util.GeoUtils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.semd.backend.dto.response.TrackingResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AmbulanceJourneyService {

    private static final Logger log = LoggerFactory.getLogger(AmbulanceJourneyService.class);

    private final AmbulanceSimulationRepository simulationRepo;
    private final SimulationLegRepository legRepo;
    private final DispatchMissionRepository missionRepo;
    private final DispatchResourceRepository resourceRepo;
    private final MedicalHospitalRepository hospitalRepo;
    private final SimulationScheduler scheduler;
    private final SimulationEventPublisher publisher;
    private final OsrmClient osrmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public AmbulanceJourneyService(
            AmbulanceSimulationRepository simulationRepo,
            SimulationLegRepository legRepo,
            DispatchMissionRepository missionRepo,
            DispatchResourceRepository resourceRepo,
            MedicalHospitalRepository hospitalRepo,
            SimulationScheduler scheduler,
            SimulationEventPublisher publisher,
            OsrmClient osrmClient) {
        this.simulationRepo = simulationRepo;
        this.legRepo = legRepo;
        this.missionRepo = missionRepo;
        this.resourceRepo = resourceRepo;
        this.hospitalRepo = hospitalRepo;
        this.scheduler = scheduler;
        this.publisher = publisher;
        this.osrmClient = osrmClient;
    }

    // ══════════════════════════════════════════════════════
    // TẠO PHIÊN MÔ PHỎNG
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse createSimulation(CreateSimulationRequest req) {

        DispatchMission mission = missionRepo.findById(req.getMissionId())
                .orElseThrow(() -> new RuntimeException("MISSION_NOT_FOUND: " + req.getMissionId()));

        DispatchResource resource = mission.getResource();
        MedicalHospital hospital = hospitalRepo.findById(req.getHospitalId())
                .orElseThrow(() -> new RuntimeException("HOSPITAL_NOT_FOUND: " + req.getHospitalId()));

        // Validate tọa độ tồn tại
        if (resource.getCurrentLocation() == null) {
            throw new RuntimeException("RESOURCE_LOCATION_MISSING: xe chưa có tọa độ hiện tại");
        }
        if (mission.getRequest().getTargetLocation() == null) {
            throw new RuntimeException("TARGET_LOCATION_MISSING: yêu cầu cấp cứu chưa có tọa độ hiện trường");
        }
        if (hospital.getLocation() == null) {
            throw new RuntimeException("HOSPITAL_LOCATION_MISSING: bệnh viện chưa có tọa độ");
        }

        // Kiểm tra xe không có phiên đang chạy
        simulationRepo.findByResourceIdAndStatusIn(
                resource.getId(),
                List.of(SimulationStatus.READY, SimulationStatus.RUNNING, SimulationStatus.STOPPED)
        ).ifPresent(s -> {
            throw new RuntimeException("SIMULATION_ALREADY_ACTIVE_FOR_RESOURCE");
        });

        // Tạo phiên
        AmbulanceSimulation sim = new AmbulanceSimulation();
        sim.setMission(mission);
        sim.setResource(resource);
        sim.setHospital(hospital);
        sim.setTickIntervalMs(req.getTickIntervalMs());
        sim.setSpeedMultiplier(BigDecimal.valueOf(req.getSpeedMultiplier()));
        sim.setSceneWaitSeconds(req.getSceneWaitSeconds());
        sim.setStatus(SimulationStatus.READY);
        sim.setPhase(SimulationPhase.TO_SCENE);
        sim.setRouteIndex(0);
        sim.setElapsedRouteMs(0L);

        AmbulanceSimulation saved = simulationRepo.save(sim);

        // ── Lấy tọa độ thật từ entity ──────────────────────
        double resourceLon = GeoUtils.lon(resource.getCurrentLocation());
        double resourceLat = GeoUtils.lat(resource.getCurrentLocation());
        double sceneLon    = GeoUtils.lon(mission.getRequest().getTargetLocation());
        double sceneLat    = GeoUtils.lat(mission.getRequest().getTargetLocation());
        double hospitalLon = GeoUtils.lon(hospital.getLocation());
        double hospitalLat = GeoUtils.lat(hospital.getLocation());

        // ── Gọi OSRM với tọa độ thật ───────────────────────
        try {
            // Chặng 1: xe → hiện trường
            OsrmRouteResponse leg1Route = osrmClient.getRoute(
                    resourceLon, resourceLat,
                    sceneLon, sceneLat
            );
            SimulationLeg leg1 = new SimulationLeg();
            leg1.setSimulation(saved);
            leg1.setLegType(LegType.TO_SCENE);
            leg1.setSequenceNo((short) 1);
            leg1.setDistanceM(BigDecimal.valueOf(leg1Route.getRoutes().get(0).getDistance()));
            leg1.setDurationS(BigDecimal.valueOf(leg1Route.getRoutes().get(0).getDuration()));
            leg1.setRoutePayload(objectMapper.writeValueAsString(
                    leg1Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg1);

            // Chặng 2: hiện trường → bệnh viện
            OsrmRouteResponse leg2Route = osrmClient.getRoute(
                    sceneLon, sceneLat,
                    hospitalLon, hospitalLat
            );
            SimulationLeg leg2 = new SimulationLeg();
            leg2.setSimulation(saved);
            leg2.setLegType(LegType.TO_HOSPITAL);
            leg2.setSequenceNo((short) 2);
            leg2.setDistanceM(BigDecimal.valueOf(leg2Route.getRoutes().get(0).getDistance()));
            leg2.setDurationS(BigDecimal.valueOf(leg2Route.getRoutes().get(0).getDuration()));
            leg2.setRoutePayload(objectMapper.writeValueAsString(
                    leg2Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg2);

            log.info("Simulation {} tạo route thành công: chặng1={}m, chặng2={}m",
                    saved.getId(),
                    leg1Route.getRoutes().get(0).getDistance().intValue(),
                    leg2Route.getRoutes().get(0).getDistance().intValue());

        } catch (Exception e) {
            // OSRM lỗi → đánh FAILED, không cho start
            saved.setStatus(SimulationStatus.FAILED);
            saved.setErrorCode("OSRM_UNAVAILABLE");
            saved.setErrorMessage(e.getMessage());
            simulationRepo.save(saved);
            throw new RuntimeException("OSRM_UNAVAILABLE: " + e.getMessage());
        }

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // BẮT ĐẦU MÔ PHỎNG — fix race condition bằng afterCommit
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse startSimulation(Long simulationId) {

        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND"));

        if (sim.getStatus() == SimulationStatus.RUNNING) {
            return toResponse(sim); // idempotent
        }

        if (sim.getStatus() != SimulationStatus.READY
                && sim.getStatus() != SimulationStatus.STOPPED) {
            throw new RuntimeException("INVALID_SIMULATION_STATE: " + sim.getStatus());
        }

        // Kiểm tra có legs chưa
        List<SimulationLeg> legs = legRepo.findBySimulationIdOrderBySequenceNo(simulationId);
        if (legs.isEmpty()) {
            throw new RuntimeException("NO_ROUTE_AVAILABLE: tạo lại simulation để có route");
        }

        // ── CHECKPOINT 1: Mission phải đang EN_ROUTE mới cho start ──
        DispatchMission mission = sim.getMission();
        if (mission.getStatus() != DispatchMissionStatus.EN_ROUTE) {
            throw new RuntimeException(
                    "MISSION_NOT_EN_ROUTE: Driver phải bắt đầu di chuyển trước. "
                            + "Trạng thái mission hiện tại: " + mission.getStatus());
        }

        sim.setStatus(SimulationStatus.RUNNING);
        sim.setStartedAt(OffsetDateTime.now());
        sim.setRouteIndex(0);
        sim.setElapsedRouteMs(0L);
        simulationRepo.save(sim);

        long intervalMs = sim.getTickIntervalMs();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduler.schedule(simulationId, () -> tick(simulationId), intervalMs);
                publisher.publishEvent(simulationId, sim.getMission().getId(),
                        sim.getResource().getId(), "SIMULATION_STARTED");
            }
        });

        return toResponse(sim);
    }



    // ══════════════════════════════════════════════════════
    // DỪNG MÔ PHỎNG
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse stopSimulation(Long simulationId) {

        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND"));

        if (sim.getStatus() == SimulationStatus.STOPPED) {
            return toResponse(sim); // idempotent
        }

        if (sim.getStatus() != SimulationStatus.RUNNING) {
            throw new RuntimeException("INVALID_SIMULATION_STATE: " + sim.getStatus());
        }

        scheduler.cancel(simulationId);
        sim.setStatus(SimulationStatus.STOPPED);
        sim.setStoppedAt(OffsetDateTime.now());
        simulationRepo.save(sim);

        publisher.publishEvent(simulationId, sim.getMission().getId(),
                sim.getResource().getId(), "SIMULATION_STOPPED");

        return toResponse(sim);
    }

    // ══════════════════════════════════════════════════════
// CHECKPOINT 3: TIẾP TỤC TỚI BỆNH VIỆN
// Điều kiện: mission TRANSPORTING + simulation AT_SCENE
// ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse continueToHospital(Long simulationId) {

        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND"));

        // Kiểm tra phase đúng
        if (sim.getPhase() != SimulationPhase.AT_SCENE) {
            throw new RuntimeException(
                    "INVALID_PHASE: Simulation phải đang ở AT_SCENE. "
                            + "Phase hiện tại: " + sim.getPhase());
        }

        // Kiểm tra status
        if (sim.getStatus() != SimulationStatus.STOPPED) {
            throw new RuntimeException(
                    "INVALID_STATUS: Simulation phải đang STOPPED tại hiện trường. "
                            + "Status hiện tại: " + sim.getStatus());
        }

        // Kiểm tra mission đang TRANSPORTING
        DispatchMission mission = sim.getMission();
        if (mission.getStatus() != DispatchMissionStatus.TRANSPORTING) {
            throw new RuntimeException(
                    "MISSION_NOT_TRANSPORTING: Driver phải bấm 'Bắt đầu vận chuyển' trước. "
                            + "Trạng thái mission hiện tại: " + mission.getStatus());
        }

        // Chuyển sang chặng 2
        sim.setPhase(SimulationPhase.TO_HOSPITAL);
        sim.setStatus(SimulationStatus.RUNNING);
        sim.setElapsedRouteMs(0L);
        simulationRepo.save(sim);

        long intervalMs = sim.getTickIntervalMs();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduler.schedule(simulationId, () -> tick(simulationId), intervalMs);
                publisher.publishEvent(simulationId, sim.getMission().getId(),
                        sim.getResource().getId(), "DEPARTED_TO_HOSPITAL");
            }
        });

        log.info("Simulation {} tiếp tục chặng TO_HOSPITAL", simulationId);

        return toResponse(sim);
    }

    // ══════════════════════════════════════════════════════
    // GET TRẠNG THÁI
    // ══════════════════════════════════════════════════════
    public SimulationResponse getSimulation(Long simulationId) {
        return toResponse(simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND")));
    }


    public TrackingResponse getTracking(Long simulationId) {
        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND"));

        // Lấy leg hiện tại
        LegType currentLegType = (sim.getPhase() == SimulationPhase.TO_SCENE
                || sim.getPhase() == SimulationPhase.AT_SCENE)
                ? LegType.TO_SCENE : LegType.TO_HOSPITAL;

        SimulationLeg currentLeg = legRepo
                .findBySimulationIdAndLegType(simulationId, currentLegType)
                .orElse(null);

        TrackingResponse res = new TrackingResponse();
        res.setSimulationId(sim.getId());
        res.setMissionId(sim.getMission().getId());
        res.setResourceId(sim.getResource().getId());
        res.setStatus(sim.getStatus().name());
        res.setPhase(sim.getPhase().name());
        res.setSourceType(sim.getSourceType());
        res.setLastUpdatedAt(sim.getLastTickAt());

        if (currentLeg != null && sim.getElapsedRouteMs() > 0) {
            double totalDurationMs = currentLeg.getDurationS().doubleValue() * 1000.0
                    / sim.getSpeedMultiplier().doubleValue();
            double[] position = interpolatePosition(
                    currentLeg, sim.getElapsedRouteMs(), totalDurationMs);

            double progressPercent = Math.min(100.0,
                    (sim.getElapsedRouteMs() / totalDurationMs) * 100.0);
            double remainingDistanceM = currentLeg.getDistanceM().doubleValue()
                    * (1.0 - progressPercent / 100.0);
            double etaSeconds = Math.max(0,
                    (totalDurationMs - sim.getElapsedRouteMs()) / 1000.0);

            res.setCurrentLongitude(position[0]);
            res.setCurrentLatitude(position[1]);
            res.setProgressPercent(progressPercent);
            res.setRemainingDistanceMeters(remainingDistanceM);
            res.setEtaSeconds(etaSeconds);
        }

        return res;
    }

    public TrackingResponse getTrackingByMission(Integer missionId) {
        AmbulanceSimulation sim = simulationRepo.findByMissionId(missionId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND_FOR_MISSION: " + missionId));
        return getTracking(sim.getId());
    }

    // ══════════════════════════════════════════════════════
    // TICK — trái tim của mô phỏng, chạy mỗi N ms
    // ══════════════════════════════════════════════════════
    private void tick(Long simulationId) {
        try {
            AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
            if (sim == null || sim.getStatus() != SimulationStatus.RUNNING) {
                scheduler.cancel(simulationId);
                return;
            }

            // Lấy leg hiện tại theo phase
            LegType currentLegType = (sim.getPhase() == SimulationPhase.TO_SCENE)
                    ? LegType.TO_SCENE : LegType.TO_HOSPITAL;

            SimulationLeg currentLeg = legRepo
                    .findBySimulationIdAndLegType(simulationId, currentLegType)
                    .orElse(null);

            if (currentLeg == null) {
                log.warn("Simulation {} không có leg {}", simulationId, currentLegType);
                scheduler.cancel(simulationId);
                return;
            }

            // Tổng thời gian chặng (ms)
            double totalDurationMs = currentLeg.getDurationS().doubleValue() * 1000.0
                    / sim.getSpeedMultiplier().doubleValue();

            // Cộng thêm thời gian đã đi
            long newElapsed = sim.getElapsedRouteMs() + sim.getTickIntervalMs();
            sim.setElapsedRouteMs(newElapsed);
            sim.setLastTickAt(OffsetDateTime.now());

            // ── Nội suy vị trí từ geometry ──────────────────
            double[] position = interpolatePosition(currentLeg, newElapsed, totalDurationMs);
            double currentLon = position[0];
            double currentLat = position[1];

            // Tính progress và ETA
            double progressPercent = Math.min(100.0, (newElapsed / totalDurationMs) * 100.0);
            double remainingMs = Math.max(0, totalDurationMs - newElapsed);
            double etaSeconds = remainingMs / 1000.0;
            double remainingDistanceM = currentLeg.getDistanceM().doubleValue()
                    * (1.0 - progressPercent / 100.0);

            // ── Kiểm tra đã đến cuối chặng chưa ────────────
            if (newElapsed >= totalDurationMs) {
                handlePhaseComplete(sim, currentLon, currentLat, simulationId);
                return;
            }

            DispatchResource resource = sim.getResource();

            Point resourcePoint = geometryFactory.createPoint(
                    new Coordinate(
                            currentLon,
                            currentLat
                    )
            );

            resourcePoint.setSRID(4326);

            resource.setCurrentLocation(resourcePoint);
            resource.setUpdatedAt(LocalDateTime.now());

            resourceRepo.save(resource);

            // Lưu trạng thái
            simulationRepo.save(sim);

            // Phát WebSocket vị trí thật
            publisher.publishPosition(
                    simulationId,
                    sim.getMission().getId(),
                    sim.getResource().getId(),
                    sim.getStatus().name(),
                    sim.getPhase().name(),
                    currentLon, currentLat,
                    progressPercent, remainingDistanceM, etaSeconds
            );

        } catch (Exception e) {
            log.error("Tick error simulation {}: {}", simulationId, e.getMessage());
            markFailed(simulationId, e.getMessage());
        }
    }

    // ── Xử lý khi hoàn thành một chặng ─────────────────────────────────────
    private void handlePhaseComplete(AmbulanceSimulation sim,
                                     double currentLon, double currentLat,
                                     Long simulationId) {
        DispatchResource resource = sim.getResource();

        Point resourcePoint = geometryFactory.createPoint(
                new Coordinate(currentLon, currentLat)
        );

        resourcePoint.setSRID(4326);

        resource.setCurrentLocation(resourcePoint);
        resource.setUpdatedAt(LocalDateTime.now());

        resourceRepo.save(resource);
        if (sim.getPhase() == SimulationPhase.TO_SCENE) {

            // ── CHECKPOINT 2: Dừng hẳn tại AT_SCENE, không tự chạy tiếp ──
            sim.setPhase(SimulationPhase.AT_SCENE);
            sim.setElapsedRouteMs(0L);
            sim.setStatus(SimulationStatus.STOPPED); // dừng, chờ Driver TRANSPORTING
            simulationRepo.save(sim);

            scheduler.cancel(simulationId);

            publisher.publishPosition(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(),
                    SimulationStatus.STOPPED.name(),
                    SimulationPhase.AT_SCENE.name(),
                    currentLon, currentLat, 100.0, 0.0, 0.0);

            publisher.publishEvent(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(), "ARRIVED_AT_SCENE");

            log.info("Simulation {} dừng tại AT_SCENE, chờ Driver chuyển TRANSPORTING", simulationId);

        } else if (sim.getPhase() == SimulationPhase.TO_HOSPITAL) {

            sim.setPhase(SimulationPhase.ARRIVED_HOSPITAL);
            sim.setStatus(SimulationStatus.COMPLETED);
            sim.setCompletedAt(OffsetDateTime.now());
            simulationRepo.save(sim);

            scheduler.cancel(simulationId);

            publisher.publishPosition(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(),
                    SimulationStatus.COMPLETED.name(),
                    SimulationPhase.ARRIVED_HOSPITAL.name(),
                    currentLon, currentLat, 100.0, 0.0, 0.0);

            publisher.publishEvent(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(), "SIMULATION_COMPLETED");

            log.info("Simulation {} hoàn thành hành trình", simulationId);
        }
    }

    // ── Bắt đầu chặng 2: hiện trường → bệnh viện ───────────────────────────
    private void startToHospitalPhase(Long simulationId) {
        try {
            AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
            if (sim == null) return;

            sim.setPhase(SimulationPhase.TO_HOSPITAL);
            sim.setElapsedRouteMs(0L);
            simulationRepo.save(sim);

            publisher.publishEvent(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(), "DEPARTED_TO_HOSPITAL");

            scheduler.schedule(simulationId, () -> tick(simulationId), sim.getTickIntervalMs());

        } catch (Exception e) {
            log.error("startToHospitalPhase error: {}", e.getMessage());
            markFailed(simulationId, e.getMessage());
        }
    }

    // ── Nội suy vị trí trên đường OSRM ─────────────────────────────────────
    private double[] interpolatePosition(SimulationLeg leg,
                                         long elapsedMs,
                                         double totalDurationMs) {
        try {
            // Parse coordinates từ routePayload JSON
            String payload = leg.getRoutePayload();
            Map<String, Object> geometry = objectMapper.readValue(
                    payload, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");

            if (coordinates == null || coordinates.isEmpty()) {
                return defaultPosition(leg);
            }

            // Tính tỉ lệ đã đi (0.0 → 1.0)
            double ratio = Math.min(1.0, elapsedMs / totalDurationMs);
            double targetIndex = ratio * (coordinates.size() - 1);
            int idx = (int) targetIndex;

            if (idx >= coordinates.size() - 1) {
                List<Double> last = coordinates.get(coordinates.size() - 1);
                return new double[]{last.get(0), last.get(1)};
            }

            // Nội suy tuyến tính giữa 2 điểm
            double frac = targetIndex - idx;
            List<Double> p1 = coordinates.get(idx);
            List<Double> p2 = coordinates.get(idx + 1);

            double lon = p1.get(0) + frac * (p2.get(0) - p1.get(0));
            double lat = p1.get(1) + frac * (p2.get(1) - p1.get(1));

            return new double[]{lon, lat};

        } catch (Exception e) {
            log.warn("Không parse được geometry, dùng tọa độ mặc định: {}", e.getMessage());
            return defaultPosition(leg);
        }
    }

    // Fallback nếu parse geometry lỗi
    private double[] defaultPosition(SimulationLeg leg) {
        return new double[]{105.8342, 21.0278};
    }

    // ── Đánh FAILED khi tick lỗi ────────────────────────────────────────────
    private void markFailed(Long simulationId, String errorMessage) {
        try {
            AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
            if (sim != null) {
                sim.setStatus(SimulationStatus.FAILED);
                sim.setErrorCode("TICK_ERROR");
                sim.setErrorMessage(errorMessage);
                simulationRepo.save(sim);
                publisher.publishEvent(simulationId, sim.getMission().getId(),
                        sim.getResource().getId(), "SIMULATION_FAILED");
            }
        } finally {
            scheduler.cancel(simulationId);
        }
    }

    // ── toResponse ───────────────────────────────────────────────────────────
    private SimulationResponse toResponse(AmbulanceSimulation s) {
        SimulationResponse res = new SimulationResponse();
        res.setId(s.getId());
        res.setMissionId(s.getMission().getId());
        res.setResourceId(s.getResource().getId());
        res.setHospitalId(s.getHospital().getId());
        res.setSourceType(s.getSourceType());
        res.setStatus(s.getStatus().name());
        res.setPhase(s.getPhase().name());
        res.setTickIntervalMs(s.getTickIntervalMs());
        res.setSpeedMultiplier(s.getSpeedMultiplier().doubleValue());
        res.setStartedAt(s.getStartedAt());
        res.setCompletedAt(s.getCompletedAt());
        res.setErrorCode(s.getErrorCode());
        res.setErrorMessage(s.getErrorMessage());

        // Gán vị trí hiện tại nếu đang chạy
        if (s.getElapsedRouteMs() > 0) {
            LegType legType = (s.getPhase() == SimulationPhase.TO_SCENE
                    || s.getPhase() == SimulationPhase.AT_SCENE)
                    ? LegType.TO_SCENE : LegType.TO_HOSPITAL;
            legRepo.findBySimulationIdAndLegType(s.getId(), legType).ifPresent(leg -> {
                double totalDurationMs = leg.getDurationS().doubleValue() * 1000.0
                        / s.getSpeedMultiplier().doubleValue();
                double[] pos = interpolatePosition(leg, s.getElapsedRouteMs(), totalDurationMs);
                res.setCurrentLongitude(pos[0]);
                res.setCurrentLatitude(pos[1]);
            });
        }

        return res;
    }
}