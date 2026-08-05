package com.semd.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semd.backend.client.osrm.OsrmClient;
import com.semd.backend.client.osrm.OsrmRouteResponse;
import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.dto.response.TrackingResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.repository.*;
import com.semd.backend.util.GeoUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AmbulanceJourneyService {

    private static final Logger log = LoggerFactory.getLogger(AmbulanceJourneyService.class);

    // Cập nhật DB vị trí mỗi 5 tick
    private static final int LOCATION_UPDATE_EVERY_N_TICKS = 5;
    // Ghi log vị trí mỗi 10 tick
    private static final int LOCATION_LOG_EVERY_N_TICKS = 10;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final AtomicLong locationLogSequence = new AtomicLong(0);

    private final AmbulanceSimulationRepository simulationRepo;
    private final SimulationLegRepository legRepo;
    private final DispatchMissionRepository missionRepo;
    private final DispatchResourceRepository resourceRepo;
    private final MedicalHospitalRepository hospitalRepo;
    private final ResourceLocationLogRepository locationLogRepo;
    private final SimulationScheduler scheduler;
    private final SimulationEventPublisher publisher;
    private final OsrmClient osrmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AmbulanceJourneyService(
            AmbulanceSimulationRepository simulationRepo,
            SimulationLegRepository legRepo,
            DispatchMissionRepository missionRepo,
            DispatchResourceRepository resourceRepo,
            MedicalHospitalRepository hospitalRepo,
            ResourceLocationLogRepository locationLogRepo,
            SimulationScheduler scheduler,
            SimulationEventPublisher publisher,
            OsrmClient osrmClient) {
        this.simulationRepo = simulationRepo;
        this.legRepo = legRepo;
        this.missionRepo = missionRepo;
        this.resourceRepo = resourceRepo;
        this.hospitalRepo = hospitalRepo;
        this.locationLogRepo = locationLogRepo;
        this.scheduler = scheduler;
        this.publisher = publisher;
        this.osrmClient = osrmClient;
    }

    // ══════════════════════════════════════════════════════
    // TẠO PHIÊN — gọi OSRM TRƯỚC khi lưu DB
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse createSimulation(CreateSimulationRequest req) {

        DispatchMission mission = missionRepo.findById(req.getMissionId())
                .orElseThrow(() -> new SimulationException(404, "MISSION_NOT_FOUND",
                        "Không tìm thấy mission: " + req.getMissionId()));

        // Kiểm tra trạng thái mission hợp lệ
        if (mission.getStatus() != DispatchMissionStatus.ACCEPTED
                && mission.getStatus() != DispatchMissionStatus.EN_ROUTE) {
            throw new BusinessConflictException(
                    "Mission phải ở trạng thái ACCEPTED hoặc EN_ROUTE để tạo simulation. "
                            + "Trạng thái hiện tại: " + mission.getStatus());
        }

        // Chặn duplicate simulation theo mission
        simulationRepo.findActivByMissionId(
                mission.getId(),
                Set.of(SimulationStatus.READY, SimulationStatus.RUNNING, SimulationStatus.STOPPED)
        ).ifPresent(s -> {
            throw new BusinessConflictException("Mission đã có simulation đang hoạt động: " + s.getId());
        });

        DispatchResource resource = mission.getResource();
        MedicalHospital hospital = hospitalRepo.findById(req.getHospitalId())
                .orElseThrow(() -> new SimulationException(404, "HOSPITAL_NOT_FOUND",
                        "Không tìm thấy bệnh viện: " + req.getHospitalId()));

        // Validate tọa độ — trả 422
        if (resource.getCurrentLocation() == null) {
            throw new SimulationException(422, "RESOURCE_LOCATION_MISSING",
                    "Xe chưa có tọa độ hiện tại");
        }
        if (mission.getRequest().getTargetLocation() == null) {
            throw new SimulationException(422, "TARGET_LOCATION_MISSING",
                    "Yêu cầu cấp cứu chưa có tọa độ hiện trường");
        }
        if (hospital.getLocation() == null) {
            throw new SimulationException(422, "HOSPITAL_LOCATION_MISSING",
                    "Bệnh viện chưa có tọa độ");
        }

        // Lấy tọa độ thật
        double resourceLon = GeoUtils.lon(resource.getCurrentLocation());
        double resourceLat = GeoUtils.lat(resource.getCurrentLocation());
        double sceneLon    = GeoUtils.lon(mission.getRequest().getTargetLocation());
        double sceneLat    = GeoUtils.lat(mission.getRequest().getTargetLocation());
        double hospitalLon = GeoUtils.lon(hospital.getLocation());
        double hospitalLat = GeoUtils.lat(hospital.getLocation());

        // ── Gọi OSRM TRƯỚC khi lưu bất kỳ thứ gì ──────────
        // Nếu OSRM lỗi → trả 502, không tạo dữ liệu dở dang
        OsrmRouteResponse leg1Route;
        OsrmRouteResponse leg2Route;
        try {
            leg1Route = osrmClient.getRoute(resourceLon, resourceLat, sceneLon, sceneLat);
            leg2Route = osrmClient.getRoute(sceneLon, sceneLat, hospitalLon, hospitalLat);
        } catch (Exception e) {
            throw new SimulationException(502, "OSRM_UNAVAILABLE",
                    "Không thể tính đường đi: " + e.getMessage());
        }

        // ── Lưu simulation sau khi OSRM thành công ─────────
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

        // Lưu 2 legs
        try {
            SimulationLeg leg1 = new SimulationLeg();
            leg1.setSimulation(saved);
            leg1.setLegType(LegType.TO_SCENE);
            leg1.setSequenceNo((short) 1);
            leg1.setDistanceM(BigDecimal.valueOf(leg1Route.getRoutes().get(0).getDistance()));
            leg1.setDurationS(BigDecimal.valueOf(leg1Route.getRoutes().get(0).getDuration()));
            leg1.setRoutePayload(objectMapper.writeValueAsString(
                    leg1Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg1);

            SimulationLeg leg2 = new SimulationLeg();
            leg2.setSimulation(saved);
            leg2.setLegType(LegType.TO_HOSPITAL);
            leg2.setSequenceNo((short) 2);
            leg2.setDistanceM(BigDecimal.valueOf(leg2Route.getRoutes().get(0).getDistance()));
            leg2.setDurationS(BigDecimal.valueOf(leg2Route.getRoutes().get(0).getDuration()));
            leg2.setRoutePayload(objectMapper.writeValueAsString(
                    leg2Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg2);

        } catch (Exception e) {
            throw new SimulationException(500, "LEG_SAVE_ERROR",
                    "Lưu route thất bại: " + e.getMessage());
        }

        log.info("Simulation {} tạo thành công, chặng1={}m chặng2={}m",
                saved.getId(),
                leg1Route.getRoutes().get(0).getDistance().intValue(),
                leg2Route.getRoutes().get(0).getDistance().intValue());

        return toResponse(saved);
    }

    // ══════════════════════════════════════════════════════
    // BẮT ĐẦU / TIẾP TỤC — fix race condition + stop/resume
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse startSimulation(Long simulationId) {

        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new SimulationException(404, "SIMULATION_NOT_FOUND",
                        "Không tìm thấy simulation: " + simulationId));

        if (sim.getStatus() == SimulationStatus.RUNNING) {
            return toResponse(sim); // idempotent
        }

        if (sim.getStatus() != SimulationStatus.READY
                && sim.getStatus() != SimulationStatus.STOPPED) {
            throw new SimulationException(409, "INVALID_SIMULATION_STATE",
                    "Không thể start khi đang ở trạng thái: " + sim.getStatus());
        }

        List<SimulationLeg> legs = legRepo.findBySimulationIdOrderBySequenceNo(simulationId);
        if (legs.isEmpty()) {
            throw new SimulationException(409, "NO_ROUTE_AVAILABLE",
                    "Tạo lại simulation để có route");
        }

        // Fix stop/resume: chỉ reset khi READY, giữ nguyên khi STOPPED
        if (sim.getStatus() == SimulationStatus.READY) {
            sim.setRouteIndex(0);
            sim.setElapsedRouteMs(0L);
            sim.setStartedAt(OffsetDateTime.now());
        }
        // STOPPED → giữ nguyên phase, routeIndex, elapsedRouteMs

        sim.setStatus(SimulationStatus.RUNNING);
        simulationRepo.save(sim);

        long intervalMs = sim.getTickIntervalMs();

        // Đăng ký scheduler SAU KHI transaction commit — tránh race condition
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
    // DỪNG
    // ══════════════════════════════════════════════════════
    @Transactional
    public SimulationResponse stopSimulation(Long simulationId) {

        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new SimulationException(404, "SIMULATION_NOT_FOUND",
                        "Không tìm thấy simulation: " + simulationId));

        if (sim.getStatus() == SimulationStatus.STOPPED) {
            return toResponse(sim); // idempotent
        }

        if (sim.getStatus() != SimulationStatus.RUNNING) {
            throw new SimulationException(409, "INVALID_SIMULATION_STATE",
                    "Không thể stop khi đang ở trạng thái: " + sim.getStatus());
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
    // GET
    // ══════════════════════════════════════════════════════
    public SimulationResponse getSimulation(Long simulationId) {
        return toResponse(simulationRepo.findById(simulationId)
                .orElseThrow(() -> new SimulationException(404, "SIMULATION_NOT_FOUND",
                        "Không tìm thấy simulation: " + simulationId)));
    }

    // ══════════════════════════════════════════════════════
    // TRACKING — luôn trả tọa độ, không phụ thuộc elapsed > 0
    // ══════════════════════════════════════════════════════
    public TrackingResponse getTracking(Long simulationId) {
        AmbulanceSimulation sim = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new SimulationException(404, "SIMULATION_NOT_FOUND",
                        "Không tìm thấy simulation: " + simulationId));

        LegType currentLegType = resolveCurrentLegType(sim.getPhase());
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

        if (currentLeg != null) {
            double totalDurationMs = currentLeg.getDurationS().doubleValue() * 1000.0
                    / sim.getSpeedMultiplier().doubleValue();

            // Luôn tính vị trí theo phase — không check elapsed > 0
            double[] position = resolveCurrentPosition(sim, currentLeg, totalDurationMs);

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
                .orElseThrow(() -> new SimulationException(404, "SIMULATION_NOT_FOUND",
                        "Không tìm thấy simulation cho mission: " + missionId));
        return getTracking(sim.getId());
    }

    // ══════════════════════════════════════════════════════
    // TICK
    // ══════════════════════════════════════════════════════
    private void tick(Long simulationId) {
        try {
            AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
            if (sim == null || sim.getStatus() != SimulationStatus.RUNNING) {
                scheduler.cancel(simulationId);
                return;
            }

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

            double totalDurationMs = currentLeg.getDurationS().doubleValue() * 1000.0
                    / sim.getSpeedMultiplier().doubleValue();

            long newElapsed = sim.getElapsedRouteMs() + sim.getTickIntervalMs();
            sim.setElapsedRouteMs(newElapsed);
            sim.setLastTickAt(OffsetDateTime.now());

            double[] position = interpolatePosition(currentLeg, newElapsed, totalDurationMs);
            double currentLon = position[0];
            double currentLat = position[1];

            double progressPercent = Math.min(100.0, (newElapsed / totalDurationMs) * 100.0);
            double remainingMs     = Math.max(0, totalDurationMs - newElapsed);
            double etaSeconds      = remainingMs / 1000.0;
            double remainingDistM  = currentLeg.getDistanceM().doubleValue()
                    * (1.0 - progressPercent / 100.0);

            // ── Kiểm tra đến cuối chặng ─────────────────────
            if (newElapsed >= totalDurationMs) {
                handlePhaseComplete(sim, currentLon, currentLat, simulationId);
                return;
            }

            simulationRepo.save(sim);

            // ── Cập nhật resource.currentLocation mỗi 5 tick ─
            long tickCount = newElapsed / sim.getTickIntervalMs();
            if (tickCount % LOCATION_UPDATE_EVERY_N_TICKS == 0) {
                updateResourceLocation(sim, currentLon, currentLat);
            }

            // ── Ghi location log mỗi 10 tick ─────────────────
            if (tickCount % LOCATION_LOG_EVERY_N_TICKS == 0) {
                saveLocationLog(sim, currentLon, currentLat);
            }

            // ── Phát WebSocket ───────────────────────────────
            publisher.publishPosition(
                    simulationId,
                    sim.getMission().getId(),
                    sim.getResource().getId(),
                    sim.getStatus().name(),
                    sim.getPhase().name(),
                    currentLon, currentLat,
                    progressPercent, remainingDistM, etaSeconds
            );

        } catch (Exception e) {
            log.error("Tick error simulation {}: {}", simulationId, e.getMessage());
            markFailed(simulationId, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPER — cập nhật vị trí xe trong DB
    // ══════════════════════════════════════════════════════
    private void updateResourceLocation(AmbulanceSimulation sim,
                                        double lon, double lat) {
        try {
            DispatchResource resource = sim.getResource();
            resource.setCurrentLocation(createPoint(lon, lat));
            resource.setUpdatedAt(LocalDateTime.now());
            resourceRepo.save(resource);
        } catch (Exception e) {
            log.warn("Không cập nhật được resource location: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPER — ghi lịch sử vị trí
    // ══════════════════════════════════════════════════════
    private void saveLocationLog(AmbulanceSimulation sim,
                                 double lon, double lat) {
        try {
            ResourceLocationLog locationLog = new ResourceLocationLog();
            locationLog.setResource(sim.getResource());
            locationLog.setMission(sim.getMission());
            locationLog.setSimulation(sim);
            locationLog.setSourceType("SIMULATION");
            locationLog.setLocation(createPoint(lon, lat));
            locationLog.setSequenceNo(locationLogSequence.incrementAndGet());
            locationLog.setRecordedAt(LocalDateTime.now());
            locationLogRepo.save(locationLog);
        } catch (Exception e) {
            log.warn("Không ghi được location log: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    // HELPER — tạo Point JTS
    // ══════════════════════════════════════════════════════
    private Point createPoint(double longitude, double latitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    // ══════════════════════════════════════════════════════
    // HELPER — xử lý chuyển phase
    // ══════════════════════════════════════════════════════
    private void handlePhaseComplete(AmbulanceSimulation sim,
                                     double currentLon, double currentLat,
                                     Long simulationId) {
        if (sim.getPhase() == SimulationPhase.TO_SCENE) {
            sim.setPhase(SimulationPhase.AT_SCENE);
            sim.setElapsedRouteMs(0L);
            simulationRepo.save(sim);

            // Cập nhật vị trí xe tại hiện trường
            updateResourceLocation(sim, currentLon, currentLat);
            saveLocationLog(sim, currentLon, currentLat);

            publisher.publishEvent(simulationId, sim.getMission().getId(),
                    sim.getResource().getId(), "ARRIVED_AT_SCENE");

            long waitMs = (long) sim.getSceneWaitSeconds() * 1000L;
            scheduler.cancel(simulationId);
            scheduler.scheduleOnce(simulationId,
                    () -> startToHospitalPhase(simulationId), waitMs);

        } else if (sim.getPhase() == SimulationPhase.TO_HOSPITAL) {
            sim.setPhase(SimulationPhase.ARRIVED_HOSPITAL);
            sim.setStatus(SimulationStatus.COMPLETED);
            sim.setCompletedAt(OffsetDateTime.now());
            simulationRepo.save(sim);

            // Cập nhật vị trí xe tại bệnh viện
            updateResourceLocation(sim, currentLon, currentLat);
            saveLocationLog(sim, currentLon, currentLat);

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

    // ══════════════════════════════════════════════════════
    // HELPER — resolve vị trí hiện tại theo phase
    // Không phụ thuộc elapsed > 0
    // ══════════════════════════════════════════════════════
    private double[] resolveCurrentPosition(AmbulanceSimulation sim,
                                            SimulationLeg leg,
                                            double totalDurationMs) {
        // AT_SCENE hoặc ARRIVED_HOSPITAL → trả điểm cuối chặng
        if (sim.getPhase() == SimulationPhase.AT_SCENE
                || sim.getPhase() == SimulationPhase.ARRIVED_HOSPITAL) {
            return interpolatePosition(leg, (long) totalDurationMs, totalDurationMs);
        }
        return interpolatePosition(leg, Math.max(0L, sim.getElapsedRouteMs()), totalDurationMs);
    }

    private LegType resolveCurrentLegType(SimulationPhase phase) {
        return (phase == SimulationPhase.TO_SCENE || phase == SimulationPhase.AT_SCENE)
                ? LegType.TO_SCENE : LegType.TO_HOSPITAL;
    }

    // ══════════════════════════════════════════════════════
    // HELPER — nội suy vị trí trên geometry OSRM
    // ══════════════════════════════════════════════════════
    private double[] interpolatePosition(SimulationLeg leg,
                                         long elapsedMs,
                                         double totalDurationMs) {
        try {
            String payload = leg.getRoutePayload();
            Map<String, Object> geometry = objectMapper.readValue(
                    payload, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<List<Double>> coordinates = (List<List<Double>>) geometry.get("coordinates");

            if (coordinates == null || coordinates.isEmpty()) {
                return defaultPosition(leg);
            }

            double ratio = Math.min(1.0, (double) elapsedMs / totalDurationMs);
            double targetIndex = ratio * (coordinates.size() - 1);
            int idx = (int) targetIndex;

            if (idx >= coordinates.size() - 1) {
                List<Double> last = coordinates.get(coordinates.size() - 1);
                return new double[]{last.get(0), last.get(1)};
            }

            double frac = targetIndex - idx;
            List<Double> p1 = coordinates.get(idx);
            List<Double> p2 = coordinates.get(idx + 1);

            double lon = p1.get(0) + frac * (p2.get(0) - p1.get(0));
            double lat = p1.get(1) + frac * (p2.get(1) - p1.get(1));

            return new double[]{lon, lat};

        } catch (Exception e) {
            log.warn("Không parse được geometry: {}", e.getMessage());
            return defaultPosition(leg);
        }
    }

    // ── Fallback: dùng vị trí hiện tại của xe thay vì tọa độ cố định ───────
    private double[] defaultPosition(SimulationLeg leg) {
        // Thử lấy từ simulation → resource
        try {
            AmbulanceSimulation sim = simulationRepo.findById(
                    leg.getSimulation().getId()).orElse(null);
            if (sim != null && sim.getResource().getCurrentLocation() != null) {
                Point current = sim.getResource().getCurrentLocation();
                return new double[]{current.getX(), current.getY()};
            }
        } catch (Exception ignored) {}

        throw new IllegalStateException(
                "Không xác định được vị trí dự phòng cho leg: " + leg.getId());
    }

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

    // ══════════════════════════════════════════════════════
    // toResponse
    // ══════════════════════════════════════════════════════
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

        // Gán vị trí hiện tại theo phase
        LegType legType = resolveCurrentLegType(s.getPhase());
        legRepo.findBySimulationIdAndLegType(s.getId(), legType).ifPresent(leg -> {
            double totalDurationMs = leg.getDurationS().doubleValue() * 1000.0
                    / s.getSpeedMultiplier().doubleValue();
            double[] pos = resolveCurrentPosition(s, leg, totalDurationMs);
            res.setCurrentLongitude(pos[0]);
            res.setCurrentLatitude(pos[1]);
        });

        return res;
    }

    // ══════════════════════════════════════════════════════
    // Exception nội bộ với HTTP status rõ ràng
    // ══════════════════════════════════════════════════════
    public static class SimulationException extends RuntimeException {
        private final int httpStatus;
        private final String errorCode;

        public SimulationException(int httpStatus, String errorCode, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.errorCode = errorCode;
        }

        public int getHttpStatus() { return httpStatus; }
        public String getErrorCode() { return errorCode; }
    }
}