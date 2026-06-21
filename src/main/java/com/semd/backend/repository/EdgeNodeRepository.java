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
}
