package com.semd.backend.repository;

import com.semd.backend.entity.ResourceLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceLocationLogRepository
        extends JpaRepository<ResourceLocationLog, Long> {
}