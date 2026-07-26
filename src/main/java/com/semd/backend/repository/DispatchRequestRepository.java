package com.semd.backend.repository;

import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.entity.DispatchRequestStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchRequestRepository extends JpaRepository<DispatchRequest, Integer>, JpaSpecificationExecutor<DispatchRequest> {

    List<DispatchRequest> findByStatus(DispatchRequestStatus status, Sort sort);

    List<DispatchRequest> findByOperationZoneId(Integer zoneId, Sort sort);

    List<DispatchRequest> findByStatusAndOperationZoneId(DispatchRequestStatus status, Integer zoneId, Sort sort);

    long countByStatus(DispatchRequestStatus status);

    @Query("SELECT COUNT(r) FROM DispatchRequest r WHERE r.status = :status AND r.createdAt >= :from")
    long countByStatusAndCreatedAtAfter(@Param("status") DispatchRequestStatus status,
                                        @Param("from") LocalDateTime from);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DispatchRequest r WHERE r.id = :id")
    Optional<DispatchRequest> findByIdForUpdate(@Param("id") Integer id);
}
