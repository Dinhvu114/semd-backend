package com.semd.backend.repository;

import com.semd.backend.entity.DispatchMission;
import com.semd.backend.entity.DispatchMissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface DispatchMissionRepository extends JpaRepository<DispatchMission, Integer> {

    Optional<DispatchMission> findByRequestId(Integer requestId);

    List<DispatchMission> findAllByRequestId(Integer requestId);

    @Query("SELECT m FROM DispatchMission m WHERE m.request.id = :requestId " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED', 'TIMEOUT', 'REJECTED')")
    Optional<DispatchMission> findActiveMissionByRequestId(@Param("requestId") Integer requestId);

    long countByRequestId(Integer requestId);

    // ── THÊM MỚI ──────────────────────────────────────────
    boolean existsByRequestId(Integer requestId);

    @Query("SELECT COUNT(m) > 0 FROM DispatchMission m " +
            "WHERE m.resource.id = :resourceId " +
            "AND m.status IN :statuses")
    boolean existsByResourceIdAndStatusIn(
            @Param("resourceId") Integer resourceId,
            @Param("statuses") List<DispatchMissionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM DispatchMission m WHERE m.id = :id")
    Optional<DispatchMission> findByIdWithLock(@Param("id") Integer id);

    @Query("SELECT m FROM DispatchMission m " +
            "WHERE m.resource.currentDriver.id = :driverId " +
            "AND m.status NOT IN ('COMPLETED','CANCELLED','REJECTED')")
    List<DispatchMission> findActiveMissionsByDriverId(@Param("driverId") Integer driverId);

    @Query("SELECT m FROM DispatchMission m " +
            "WHERE m.resource.currentDriver.id = :driverId ORDER BY m.dispatchedAt DESC")
    List<DispatchMission> findAllByDriverId(@Param("driverId") Integer driverId);

    @Query("SELECT m FROM DispatchMission m WHERE m.id = :missionId " +
            "AND m.resource.currentDriver.id = :driverId")
    Optional<DispatchMission> findByIdAndDriverId(
            @Param("missionId") Integer missionId,
            @Param("driverId") Integer driverId);

    @Query("SELECT m FROM DispatchMission m WHERE m.resource.currentDriver.id = :driverId " +
            "AND m.status IN :statuses ORDER BY m.dispatchedAt DESC")
    List<DispatchMission> findByDriverIdAndStatusIn(
            @Param("driverId") Integer driverId,
            @Param("statuses") List<DispatchMissionStatus> statuses);

}
