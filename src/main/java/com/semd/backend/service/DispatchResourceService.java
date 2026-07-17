package com.semd.backend.service;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.entity.*;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.DispatchResourceRepository;
import jakarta.persistence.EntityManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatchResourceService {

    private final DispatchResourceRepository repository;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public DispatchResourceService(DispatchResourceRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional
    public DispatchResourceDto createResource(DispatchResourceRequest request) {
        if (repository.existsByResourceCode(request.resourceCode())) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã tồn tại");
        }

        DispatchResource resource = new DispatchResource();
        resource.setResourceCode(request.resourceCode());
        resource.setStatus(request.status());
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        // Map relationships
        setRelationships(resource, request);

        DispatchResource saved = repository.save(resource);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DispatchResourceDto> getAllResources() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DispatchResourceDto getResourceById(Integer id) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));
        return mapToDto(resource);
    }

    @Transactional
    public DispatchResourceDto updateResource(Integer id, DispatchResourceRequest request) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));

        if (repository.existsByResourceCodeAndIdNot(request.resourceCode(), id)) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã được sử dụng bởi xe khác");
        }

        resource.setResourceCode(request.resourceCode());
        resource.setStatus(request.status());
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        // Map relationships
        setRelationships(resource, request);

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public DispatchResourceDto updateResourceStatus(Integer id, DispatchResourceStatus status) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));

        resource.setStatus(status);
        resource.setUpdatedAt(LocalDateTime.now());

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteResource(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id);
        }
        repository.deleteById(id);
    }

    private void setRelationships(DispatchResource resource, DispatchResourceRequest request) {
        if (request.resourceTypeId() != null) {
            ServiceType serviceType = entityManager.find(ServiceType.class, request.resourceTypeId());
            if (serviceType == null) {
                throw new ResourceNotFoundException("Không tìm thấy loại dịch vụ với ID: " + request.resourceTypeId());
            }
            resource.setResourceType(serviceType);
        } else {
            resource.setResourceType(null);
        }

        if (request.zoneId() != null) {
            OperationZone operationZone = entityManager.find(OperationZone.class, request.zoneId());
            if (operationZone == null) {
                throw new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + request.zoneId());
            }
            resource.setOperationZone(operationZone);
        } else {
            resource.setOperationZone(null);
        }

        if (request.providerId() != null) {
            Provider provider = entityManager.find(Provider.class, request.providerId());
            if (provider == null) {
                throw new ResourceNotFoundException("Không tìm thấy nhà xe/đơn vị với ID: " + request.providerId());
            }
            resource.setProvider(provider);
        } else {
            resource.setProvider(null);
        }

        if (request.currentDriverId() != null) {
            User driver = entityManager.find(User.class, request.currentDriverId());
            if (driver == null) {
                throw new ResourceNotFoundException("Không tìm thấy tài xế với ID: " + request.currentDriverId());
            }
            resource.setCurrentDriver(driver);
        } else {
            resource.setCurrentDriver(null);
        }

        if (request.longitude() != null && request.latitude() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude()));
            resource.setCurrentLocation(point);
        } else {
            resource.setCurrentLocation(null);
        }
    }

    private DispatchResourceDto mapToDto(DispatchResource resource) {
        Integer typeId = resource.getResourceType() != null ? resource.getResourceType().getId() : null;
        String typeName = resource.getResourceType() != null ? resource.getResourceType().getDisplayName() : null;

        Integer zoneId = resource.getOperationZone() != null ? resource.getOperationZone().getId() : null;
        String zoneName = resource.getOperationZone() != null ? resource.getOperationZone().getZoneName() : null;

        Integer providerId = resource.getProvider() != null ? (resource.getProvider().getId() != null ? resource.getProvider().getId().intValue() : null) : null;
        String providerName = resource.getProvider() != null ? resource.getProvider().getProviderName() : null;

        Integer driverId = resource.getCurrentDriver() != null ? resource.getCurrentDriver().getId() : null;
        String driverName = resource.getCurrentDriver() != null ? resource.getCurrentDriver().getFullName() : null;

        Double longitude = null;
        Double latitude = null;
        if (resource.getCurrentLocation() != null) {
            longitude = resource.getCurrentLocation().getX();
            latitude = resource.getCurrentLocation().getY();
        }

        return new DispatchResourceDto(
                resource.getId(),
                resource.getResourceCode(),
                typeId,
                typeName,
                zoneId,
                zoneName,
                providerId,
                providerName,
                driverId,
                driverName,
                resource.getStatus() != null ? resource.getStatus().name() : null,
                longitude,
                latitude,
                resource.getExtendedAttributes(),
                resource.getUpdatedAt()
        );
    }
}
