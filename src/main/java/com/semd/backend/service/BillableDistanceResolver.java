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
import org.locationtech.jts.geom.Point;

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
                log.warn("Không tìm thấy simulation cho mission {}, fallback Haversine",
                        mission.getId());
                return resolveHaversineFallback(mission);
            }

            SimulationLeg leg = legRepo
                    .findBySimulationIdAndLegType(sim.getId(), LegType.TO_HOSPITAL)
                    .orElse(null);
            if (leg == null || leg.getDistanceM() == null) {
                log.warn("Không tìm thấy leg TO_HOSPITAL cho simulation {}, fallback Haversine",
                        sim.getId());
                return resolveHaversineFallback(mission);
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
    private Optional<BigDecimal> resolveHaversineFallback(DispatchMission mission) {
        try {
            if (mission.getRequest() == null
                    || mission.getRequest().getLatitude() == null
                    || mission.getRequest().getLongitude() == null
                    || mission.getDestination() == null
                    || mission.getDestination().getLocation() == null) {
                return Optional.empty();
            }

            double sceneLat = mission.getRequest().getLatitude();
            double sceneLng = mission.getRequest().getLongitude();

            Point hospitalLocation = mission.getDestination().getLocation();

            double hospitalLng = hospitalLocation.getX();
            double hospitalLat = hospitalLocation.getY();

            double distanceKm = haversine(
                    sceneLat,
                    sceneLng,
                    hospitalLat,
                    hospitalLng
            );

            BigDecimal result = BigDecimal.valueOf(distanceKm)
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Mission {} fallback Scene -> Hospital = {} km",
                    mission.getId(), result);

            return Optional.of(result);

        } catch (Exception e) {
            log.warn("Không tính được fallback distance cho mission {}: {}",
                    mission.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private double haversine(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return earthRadiusKm * c;
    }
}