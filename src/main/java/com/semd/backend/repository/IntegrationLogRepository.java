package com.semd.backend.repository;

import com.semd.backend.entity.IntegrationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegrationLogRepository extends JpaRepository<IntegrationLog, Long> {
}
