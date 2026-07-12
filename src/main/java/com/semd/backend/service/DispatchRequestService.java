package com.semd.backend.service;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.entity.DispatchRequest;
import com.semd.backend.repository.DispatchRequestRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatchRequestService {

    private final DispatchRequestRepository requestRepository;

    public DispatchRequestService(DispatchRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<DispatchRequestDto> getAllRequests() {
        return requestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
        Integer edgeNodeId = req.getEdgeNode() != null ? req.getEdgeNode().getId() : null;
        String edgeNodeName = req.getEdgeNode() != null ? req.getEdgeNode().getNodeName() : null;
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
                edgeNodeId,
                edgeNodeName,
                dispatcherId,
                dispatcherName,
                req.getUrgencyLevel(),
                longitude,
                latitude,
                req.getStatus(),
                req.getExtendedRequirements(),
                req.getIsSyncedToCloud(),
                req.getCreatedAt()
        );
    }
}
