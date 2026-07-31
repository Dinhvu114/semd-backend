package com.semd.backend.repository;

import com.semd.backend.entity.DispatchMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispatchMissionRepository extends JpaRepository<DispatchMission, Integer> {

    Optional<DispatchMission> findByRequestId(Integer requestId);

    List<DispatchMission> findAllByRequestId(Integer requestId);

    @Query("SELECT m FROM DispatchMission m WHERE m.request.id = :requestId " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED', 'TIMEOUT', 'DECLINED')")
    Optional<DispatchMission> findActiveMissionByRequestId(@Param("requestId") Integer requestId);

    long countByRequestId(Integer requestId);
}