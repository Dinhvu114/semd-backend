package com.semd.backend.repository;

import com.semd.backend.entity.DispatchResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DispatchResourceRepository extends JpaRepository<DispatchResource, Integer>, JpaSpecificationExecutor<DispatchResource> {
    Optional<DispatchResource> findByResourceCode(String resourceCode);
    boolean existsByResourceCode(String resourceCode);
    boolean existsByResourceCodeAndIdNot(String resourceCode, Integer id);
}
