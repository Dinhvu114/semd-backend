package com.semd.backend.repository;

import com.semd.backend.entity.DispatchResource;
import com.semd.backend.entity.DispatchResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface DispatchResourceRepository extends JpaRepository<DispatchResource, Integer>, JpaSpecificationExecutor<DispatchResource> {
    Optional<DispatchResource> findByResourceCode(String resourceCode);
    boolean existsByResourceCode(String resourceCode);
    boolean existsByResourceCodeAndIdNot(String resourceCode, Integer id);
    List<DispatchResource> findAllByStatus(DispatchResourceStatus status);

    // ── THÊM MỚI ──────────────────────────────────────────
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DispatchResource r WHERE r.id = :id")
    Optional<DispatchResource> findByIdWithLock(@Param("id") Integer id);

    // driver
    Optional<DispatchResource> findByCurrentDriverId(Integer driverId);

    boolean existsByCurrentDriverId(Integer driverId);

    boolean existsByCurrentDriverIdAndIdNot(
            Integer driverId,
            Integer resourceId
    );
    boolean existsByProviderId(Integer providerId);
}



