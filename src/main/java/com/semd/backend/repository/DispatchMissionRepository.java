package com.semd.backend.repository;

import com.semd.backend.entity.DispatchMission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DispatchMissionRepository extends JpaRepository<DispatchMission, Integer> {
    Optional<DispatchMission> findByRequestId(Integer requestId);
}