package com.semd.backend.service;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.repository.DispatchRequestRepository;
import com.semd.backend.entity.DispatchRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchRequestService {

    private final DispatchRequestRepository requestRepository;

    public DispatchRequestService(DispatchRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public Page<DispatchRequestDto> search(
            DispatchRequestStatus status,
            String urgencyLevel,
            Integer serviceTypeId,
            Integer operationZoneId,
            Pageable pageable) {
        Specification<DispatchRequest> specification = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (urgencyLevel != null && !urgencyLevel.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("urgencyLevel")), urgencyLevel.trim().toUpperCase()));
        }
        if (serviceTypeId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("serviceType").get("id"), serviceTypeId));
        }
        if (operationZoneId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("operationZone").get("id"), operationZoneId));
        }
        return requestRepository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public DispatchRequestDto getRequestById(Integer id) {
        DispatchRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException("Không tìm thấy dispatch_request id: " + id));
        return mapToDto(request);
    }

    private DispatchRequestDto mapToDto(DispatchRequest req) {
        Integer callId = req.getCall() != null ? req.getCall().getId() : null;
        Integer serviceTypeId = req.getServiceType() != null ? req.getServiceType().getId() : null;
        String serviceTypeName = req.getServiceType() != null ? req.getServiceType().getDisplayName() : null;
        Integer zoneId = req.getOperationZone() != null ? req.getOperationZone().getId() : null;
        String zoneName = req.getOperationZone() != null ? req.getOperationZone().getZoneName() : null;
        Integer dispatcherId = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getId() : null;
        String dispatcherName = req.getCreatedByDispatcher() != null ? req.getCreatedByDispatcher().getFullName() : null;

        Double longitude = null;
        Double latitude = null;
        if (req.getTargetLocation() != null) {
            longitude = req.getTargetLocation().getX();
            latitude = req.getTargetLocation().getY();
        }

        return new DispatchRequestDto(
                req.getId(),
                callId,
                serviceTypeId,
                serviceTypeName,
                zoneId,
                zoneName,
                dispatcherId,
                dispatcherName,
                req.getUrgencyLevel(),
                longitude,
                latitude,
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getExtendedRequirements(),
                req.getCreatedAt()
        );
    }
}
