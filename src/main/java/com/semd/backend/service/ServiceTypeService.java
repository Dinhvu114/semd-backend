package com.semd.backend.service;

import com.semd.backend.dto.ServiceTypeDto;
import com.semd.backend.dto.ServiceTypeRequest;
import com.semd.backend.entity.ServiceType;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.ServiceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceTypeService {

    private final ServiceTypeRepository repository;

    public ServiceTypeService(ServiceTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ServiceTypeDto createServiceType(ServiceTypeRequest request) {
        if (repository.existsByTypeCode(request.typeCode())) {
            throw new IllegalArgumentException("Mã loại dịch vụ '" + request.typeCode() + "' đã tồn tại trong hệ thống");
        }

        ServiceType serviceType = new ServiceType();
        serviceType.setTypeCode(request.typeCode());
        serviceType.setDisplayName(request.displayName());
        serviceType.setPriorityWeight(request.priorityWeight());

        ServiceType saved = repository.save(serviceType);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceTypeDto> getAllServiceTypes() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceTypeDto getServiceTypeById(Integer id) {
        ServiceType serviceType = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại dịch vụ với ID: " + id));
        return mapToDto(serviceType);
    }

    @Transactional
    public ServiceTypeDto updateServiceType(Integer id, ServiceTypeRequest request) {
        ServiceType serviceType = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại dịch vụ với ID: " + id));

        if (repository.existsByTypeCodeAndIdNot(request.typeCode(), id)) {
            throw new IllegalArgumentException("Mã loại dịch vụ '" + request.typeCode() + "' đã được sử dụng bởi loại dịch vụ khác");
        }

        serviceType.setTypeCode(request.typeCode());
        serviceType.setDisplayName(request.displayName());
        serviceType.setPriorityWeight(request.priorityWeight());

        ServiceType updated = repository.save(serviceType);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteServiceType(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy loại dịch vụ với ID: " + id);
        }
        repository.deleteById(id);
    }

    private ServiceTypeDto mapToDto(ServiceType serviceType) {
        return new ServiceTypeDto(
                serviceType.getId(),
                serviceType.getTypeCode(),
                serviceType.getDisplayName(),
                serviceType.getPriorityWeight()
        );
    }
}
