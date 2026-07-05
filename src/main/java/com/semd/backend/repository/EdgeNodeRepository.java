package com.semd.backend.repository;

import com.semd.backend.entity.EdgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdgeNodeRepository extends JpaRepository<EdgeNode, Integer> {
    Optional<EdgeNode> findByNodeName(String nodeName);
    boolean existsByNodeName(String nodeName);
    boolean existsByNodeNameAndIdNot(String nodeName, Integer id);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM edge_nodes WHERE is_active = true AND ST_Contains(coverage_area, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) LIMIT 1",
        nativeQuery = true
    )
    Optional<EdgeNode> findContainingNode(
        @org.springframework.data.repository.query.Param("lng") Double lng,
        @org.springframework.data.repository.query.Param("lat") Double lat
    );
}

