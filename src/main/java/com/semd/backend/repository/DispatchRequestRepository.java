package com.semd.backend.repository;

import com.semd.backend.entity.DispatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchRequestRepository extends JpaRepository<DispatchRequest, Integer> {
}
