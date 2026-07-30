package com.semd.backend.repository;

import com.semd.backend.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Integer> {
    Optional<ServiceType> findByTypeCode(String typeCode);
    boolean existsByTypeCode(String typeCode);
    boolean existsByTypeCodeAndIdNot(String typeCode, Integer id);
}
