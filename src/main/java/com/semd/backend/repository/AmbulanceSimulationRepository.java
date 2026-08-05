package com.semd.backend.repository;

import com.semd.backend.entity.AmbulanceSimulation;
import com.semd.backend.entity.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AmbulanceSimulationRepository extends JpaRepository<AmbulanceSimulation, Long> {

    // Tìm phiên đang chạy của một xe
    Optional<AmbulanceSimulation> findByResourceIdAndStatusIn(
            Integer resourceId, List<SimulationStatus> statuses);

    // Tìm tất cả phiên RUNNING (dùng cho startup recovery và watchdog)
    List<AmbulanceSimulation> findByStatus(SimulationStatus status);

    // Tìm phiên RUNNING quá hạn (watchdog)
    @Query("SELECT s FROM AmbulanceSimulation s " +
            "WHERE s.status = 'RUNNING' AND s.lastTickAt < :threshold")
    List<AmbulanceSimulation> findStaleRunningSessions(OffsetDateTime threshold);

    // Tìm theo mission
    Optional<AmbulanceSimulation> findByMissionId(Integer missionId);

    // ── THÊM MỚI: kiểm tra duplicate theo mission ─────────
    @Query("SELECT s FROM AmbulanceSimulation s " +
            "WHERE s.mission.id = :missionId " +
            "AND s.status IN :statuses")
    Optional<AmbulanceSimulation> findActivByMissionId(
            @Param("missionId") Integer missionId,
            @Param("statuses") Set<SimulationStatus> statuses);
}