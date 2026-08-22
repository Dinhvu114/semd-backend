package com.semd.backend.service;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.dto.request.UpdateResourceLocationRequest;
import com.semd.backend.dto.response.DispatchResourceResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public Page<DispatchResourceDto> search(
            String keyword,
            DispatchResourceStatus status,
            Integer serviceTypeId,
            Integer providerId,

            Pageable pageable) {
        Specification<DispatchResource> specification = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("resourceCode")), pattern));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (serviceTypeId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("resourceType").get("id"), serviceTypeId));
        }
        if (providerId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("provider").get("id"), providerId));
        }

        return repository.findAll(specification, pageable).map(this::mapToDto);
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

        // Deprecated - luôn null sau khi bỏ OperationZone
        Integer zoneId = null;
        String zoneName = null;

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

    // DRIVER

    @Transactional
    public void updateLocation(
            Integer resourceId,
            UpdateResourceLocationRequest request
    ) {

        DispatchResource resource =
                repository.findById(resourceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy phương tiện với id = "
                                                + resourceId
                                )
                        );

        Point point = geometryFactory.createPoint(
                new Coordinate(
                        request.longitude(),
                        request.latitude()
                )
        );

        point.setSRID(4326);

        resource.setCurrentLocation(point);
        resource.setUpdatedAt(LocalDateTime.now());

        repository.save(resource);
    }

    @Transactional
    public void updateMyResourceLocation(
            Integer driverId,
            UpdateResourceLocationRequest request
    ) {
        DispatchResource resource =
                repository.findByCurrentDriverId(driverId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tài xế hiện chưa được phân công phương tiện"
                                )
                        );

        Point point = geometryFactory.createPoint(
                new Coordinate(
                        request.longitude(),
                        request.latitude()
                )
        );

        point.setSRID(4326);

        resource.setCurrentLocation(point);
        resource.setUpdatedAt(LocalDateTime.now());

        repository.save(resource);
    }

    @Transactional(readOnly = true)
    public DispatchResourceResponse getMyResource(Integer driverId) {

        DispatchResource resource =
                repository.findByCurrentDriverId(driverId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Tài xế hiện chưa được phân công phương tiện"
                                )
                        );

        return toResponse(resource);
    }
    private DispatchResourceResponse toResponse(
        DispatchResource resource
    ) {
        Double latitude = null;
        Double longitude = null;

        if (resource.getCurrentLocation() != null) {
            latitude = resource.getCurrentLocation().getY();
            longitude = resource.getCurrentLocation().getX();
        }

        Integer driverId = null;
        String driverName = null;

        if (resource.getCurrentDriver() != null) {
            driverId = resource.getCurrentDriver().getId();
            driverName = resource.getCurrentDriver().getFullName();
        }

        String resourceType = null;

        if (resource.getResourceType() != null) {
            resourceType = resource.getResourceType().getDisplayName();
        }

        return new DispatchResourceResponse(
                resource.getId(),
                resource.getResourceCode(),
                resourceType,
                resource.getStatus().name(),
                driverId,
                driverName,
                latitude,
                longitude,
                resource.getExtendedAttributes(),
                resource.getUpdatedAt()
        );
    }
}
