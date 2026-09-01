package com.semd.backend.service;

import com.semd.backend.entity.AmbulanceSimulation;
import com.semd.backend.entity.DispatchMission;
import com.semd.backend.entity.LegType;
import com.semd.backend.entity.SimulationLeg;
import com.semd.backend.repository.AmbulanceSimulationRepository;
import com.semd.backend.repository.SimulationLegRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class BillableDistanceResolver {

    private static final Logger log = LoggerFactory.getLogger(BillableDistanceResolver.class);

    private final AmbulanceSimulationRepository simulationRepo;
    private final SimulationLegRepository legRepo;

    public BillableDistanceResolver(AmbulanceSimulationRepository simulationRepo,
                                    SimulationLegRepository legRepo) {
        this.simulationRepo = simulationRepo;
        this.legRepo = legRepo;
    }

    /**
     * Chỉ tính quãng đường vận chuyển bệnh nhân: hiện trường → bệnh viện (TO_HOSPITAL).
     * Trả về Optional.empty() nếu không tìm được — KHÔNG được ném exception,
     * vì billing không được phép làm Mission complete thất bại.
     */
    public Optional<BigDecimal> resolveBillableDistanceKm(DispatchMission mission) {
        try {
            AmbulanceSimulation sim = simulationRepo.findByMissionId(mission.getId())
                    .orElse(null);
            if (sim == null) {
                log.warn("Không tìm thấy simulation cho mission {}", mission.getId());
                return Optional.empty();
            }

            SimulationLeg leg = legRepo
                    .findBySimulationIdAndLegType(sim.getId(), LegType.TO_HOSPITAL)
                    .orElse(null);
            if (leg == null || leg.getDistanceM() == null) {
                log.warn("Không tìm thấy leg TO_HOSPITAL cho simulation {}", sim.getId());
                return Optional.empty();
            }

            BigDecimal distanceKm = leg.getDistanceM()
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            return Optional.of(distanceKm);

        } catch (Exception e) {
            log.error("Lỗi khi resolve billable distance cho mission {}: {}",
                    mission.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}