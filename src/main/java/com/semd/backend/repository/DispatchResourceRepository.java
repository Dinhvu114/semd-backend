package com.semd.backend.repository;

import com.semd.backend.entity.DispatchResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface DispatchResourceRepository extends JpaRepository<DispatchResource, Integer>, JpaSpecificationExecutor<DispatchResource> {
    Optional<DispatchResource> findByResourceCode(String resourceCode);
    boolean existsByResourceCode(String resourceCode);
    boolean existsByResourceCodeAndIdNot(String resourceCode, Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DispatchResource r WHERE r.id = :id")
    Optional<DispatchResource> findByIdForUpdate(@Param("id") Integer id);
}
