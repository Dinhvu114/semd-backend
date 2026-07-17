package com.semd.backend.repository;

import com.semd.backend.entity.OperationZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperationZoneRepository extends JpaRepository<OperationZone, Integer> {
    Optional<OperationZone> findByZoneName(String zoneName);
    boolean existsByZoneName(String zoneName);
    boolean existsByZoneNameAndIdNot(String zoneName, Integer id);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM operation_zones WHERE is_active = true AND ST_Contains(coverage_area, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) LIMIT 1",
        nativeQuery = true
    )
    Optional<OperationZone> findContainingZone(
        @org.springframework.data.repository.query.Param("lng") Double lng,
        @org.springframework.data.repository.query.Param("lat") Double lat
    );
}
