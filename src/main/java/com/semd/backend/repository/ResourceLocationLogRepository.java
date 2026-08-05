package com.semd.backend.repository;

import com.semd.backend.entity.ResourceLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceLocationLogRepository extends JpaRepository<ResourceLocationLog, Long> {

    List<ResourceLocationLog> findBySimulationIdOrderByRecordedAtAsc(Long simulationId);

    List<ResourceLocationLog> findByMissionIdOrderByRecordedAtAsc(Integer missionId);
}