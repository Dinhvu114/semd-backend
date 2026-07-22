package com.semd.backend.repository;

import com.semd.backend.entity.MissionStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionStatusLogRepository extends JpaRepository<MissionStatusLog, Long> {

    List<MissionStatusLog> findAllByMissionIdOrderByCreatedAtAsc(Integer missionId);
}
