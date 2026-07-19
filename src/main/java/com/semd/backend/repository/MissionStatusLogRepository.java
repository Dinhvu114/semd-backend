package com.semd.backend.repository;

import com.semd.backend.entity.MissionStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MissionStatusLogRepository extends JpaRepository<MissionStatusLog, Long> {
}
