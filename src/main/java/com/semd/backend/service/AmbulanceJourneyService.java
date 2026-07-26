package com.semd.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semd.backend.client.osrm.OsrmClient;
import com.semd.backend.client.osrm.OsrmRouteResponse;
import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.entity.*;
import com.semd.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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

    @Transactional
    public SimulationResponse createSimulation(CreateSimulationRequest req) {

        DispatchMission mission = missionRepo.findById(req.getMissionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mission: " + req.getMissionId()));

        DispatchResource resource = mission.getResource();
        MedicalHospital hospital = hospitalRepo.findById(req.getHospitalId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh viện: " + req.getHospitalId()));

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

        AmbulanceSimulation saved = simulationRepo.save(sim);

        // Gọi OSRM 2 lần cho 2 chặng (dùng tọa độ giả nếu chưa có PostGIS handler)
        // Chặng 1: xe → hiện trường (dùng Hà Nội làm mock nếu chưa có tọa độ thật)
        try {
            OsrmRouteResponse leg1Route = osrmClient.getRoute(
                    105.8342, 21.0278,  // xe (mock)
                    105.8501, 21.0322   // hiện trường (mock)
            );
            SimulationLeg leg1 = new SimulationLeg();
            leg1.setSimulation(saved);
            leg1.setLegType(LegType.TO_SCENE);
            leg1.setSequenceNo((short) 1);
            leg1.setDistanceM(BigDecimal.valueOf(
                    leg1Route.getRoutes().get(0).getDistance()));
            leg1.setDurationS(BigDecimal.valueOf(
                    leg1Route.getRoutes().get(0).getDuration()));
            leg1.setRoutePayload(objectMapper.writeValueAsString(
                    leg1Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg1);

            // Chặng 2: hiện trường → bệnh viện
            OsrmRouteResponse leg2Route = osrmClient.getRoute(
                    105.8501, 21.0322,  // hiện trường (mock)
                    105.8412, 21.0245   // bệnh viện (mock)
            );
            SimulationLeg leg2 = new SimulationLeg();
            leg2.setSimulation(saved);
            leg2.setLegType(LegType.TO_HOSPITAL);
            leg2.setSequenceNo((short) 2);
            leg2.setDistanceM(BigDecimal.valueOf(
                    leg2Route.getRoutes().get(0).getDistance()));
            leg2.setDurationS(BigDecimal.valueOf(
                    leg2Route.getRoutes().get(0).getDuration()));
            leg2.setRoutePayload(objectMapper.writeValueAsString(
                    leg2Route.getRoutes().get(0).getGeometry()));
            legRepo.save(leg2);

        } catch (Exception e) {
            log.warn("OSRM chưa khả dụng, tạo phiên không có route: {}", e.getMessage());
        }

        return toResponse(saved);
    }

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

        sim.setStatus(SimulationStatus.RUNNING);
        sim.setStartedAt(OffsetDateTime.now());
        simulationRepo.save(sim);

        // Đăng ký tick vào scheduler
        scheduler.schedule(simulationId, () -> tick(simulationId), sim.getTickIntervalMs());
        publisher.publishEvent(simulationId, sim.getMission().getId(),
                sim.getResource().getId(), "SIMULATION_STARTED");

        return toResponse(sim);
    }

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

    public SimulationResponse getSimulation(Long simulationId) {
        return toResponse(simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("SIMULATION_NOT_FOUND")));
    }

    // ── TICK — chạy mỗi N ms ────────────────────────────────────────────────
    private void tick(Long simulationId) {
        try {
            AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
            if (sim == null || sim.getStatus() != SimulationStatus.RUNNING) {
                scheduler.cancel(simulationId);
                return;
            }

            sim.setElapsedRouteMs(sim.getElapsedRouteMs()
                    + (long)(sim.getTickIntervalMs() * sim.getSpeedMultiplier().doubleValue()));
            sim.setLastTickAt(OffsetDateTime.now());
            simulationRepo.save(sim);

            // Phát WebSocket vị trí (dùng tọa độ mock, sau thay bằng nội suy thật)
            publisher.publishPosition(
                    simulationId,
                    sim.getMission().getId(),
                    sim.getResource().getId(),
                    sim.getStatus().name(),
                    sim.getPhase().name(),
                    105.8342, 21.0278, // lon, lat mock
                    50.0, 2000.0, 120.0  // progress, remaining, eta
            );

        } catch (Exception e) {
            log.error("Tick error simulation {}: {}", simulationId, e.getMessage());
            try {
                AmbulanceSimulation sim = simulationRepo.findById(simulationId).orElse(null);
                if (sim != null) {
                    sim.setStatus(SimulationStatus.FAILED);
                    sim.setErrorCode("TICK_ERROR");
                    sim.setErrorMessage(e.getMessage());
                    simulationRepo.save(sim);
                    publisher.publishEvent(simulationId, sim.getMission().getId(),
                            sim.getResource().getId(), "SIMULATION_FAILED");
                }
            } finally {
                scheduler.cancel(simulationId);
            }
        }
    }

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
        return res;
    }
}