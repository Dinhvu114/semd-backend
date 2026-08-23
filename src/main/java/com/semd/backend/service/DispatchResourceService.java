package com.semd.backend.service;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.dto.request.UpdateResourceLocationRequest;
import com.semd.backend.dto.response.DispatchResourceResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.ResourceLocationLogRepository;
import com.semd.backend.repository.UserRepository;

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

import com.semd.backend.repository.DispatchMissionRepository;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.access.AccessDeniedException;

@Service
public class DispatchResourceService {

    private final UserRepository userRepository;
    private final DispatchResourceRepository repository;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final DispatchMissionRepository missionRepository;
    private final AmbulancePositionPublisher positionPublisher;
    private final ResourceLocationLogRepository locationLogRepository;

    public DispatchResourceService(UserRepository userRepository, DispatchResourceRepository repository, EntityManager entityManager, DispatchMissionRepository missionRepository, AmbulancePositionPublisher positionPublisher, ResourceLocationLogRepository locationLogRepository) {
        this.userRepository = userRepository;
        this.repository = repository;
        this.entityManager = entityManager;
        this.missionRepository = missionRepository;
        this.positionPublisher = positionPublisher;
        this.locationLogRepository = locationLogRepository;
    }

    @Transactional
    public DispatchResourceDto createResource(DispatchResourceRequest request, Integer currentUserId) {
        if (repository.existsByResourceCode(request.resourceCode())) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã tồn tại");
        }

        DispatchResource resource = new DispatchResource();
        resource.setResourceCode(request.resourceCode());
        resource.setStatus(
        DispatchResourceStatus.AVAILABLE);
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        // Map relationships
        setRelationships(resource, request, currentUserId);

        DispatchResource saved = repository.save(resource);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<DispatchResourceDto> search(
            String keyword,
            DispatchResourceStatus status,
            Integer serviceTypeId,
            Integer requestedProviderId,
            Integer currentUserId,
            Pageable pageable) {
        User currentUser =
            requireCurrentUser(currentUserId);

        Integer effectiveProviderId =
                requestedProviderId;

        if (hasRole(currentUser, "PROVIDER_ADMIN")) {

            if (currentUser.getProvider() == null) {
                throw new AccessDeniedException(
                        "Tài khoản Provider Admin chưa được gắn nhà cung cấp"
                );
            }

        effectiveProviderId =
                currentUser.getProvider().getId();
    }
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
        if (effectiveProviderId != null) {
            Integer finalProviderId = effectiveProviderId;
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("provider").get("id"), finalProviderId));
        }

        return repository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public DispatchResourceDto getResourceById(
            Integer id,
            Integer currentUserId
    ) {
        User currentUser =
                requireCurrentUser(currentUserId);

        DispatchResource resource =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy xe cứu thương với ID: " + id
                                )
                        );

        assertProviderOwnership(
                resource,
                currentUser
        );

        return mapToDto(resource);
    }

    @Transactional
    public DispatchResourceDto updateResource(Integer id, DispatchResourceRequest request, Integer currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));

        assertProviderOwnership(resource, currentUser);

        if (repository.existsByResourceCodeAndIdNot(request.resourceCode(), id)) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã được sử dụng bởi xe khác");
        }

        resource.setResourceCode(request.resourceCode());
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        // Map relationships
        setRelationships(resource, request, currentUserId);

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public DispatchResourceDto updateResourceStatus(Integer id, DispatchResourceStatus status, Integer currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));

        assertProviderOwnership(resource, currentUser);

        if (status == DispatchResourceStatus.DISPATCHED ||
                status == DispatchResourceStatus.ON_MISSION) {
            throw new IllegalArgumentException(
                    "Không được chuyển thủ công xe sang trạng thái nhiệm vụ"
            );
        }

        if (!missionRepository.findActiveMissionsByResourceId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Xe đang có nhiệm vụ, không thể thay đổi trạng thái thủ công"
            );
        }

        resource.setStatus(status);
        resource.setUpdatedAt(LocalDateTime.now());

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteResource(Integer id, Integer currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe cứu thương với ID: " + id
                ));

        assertProviderOwnership(resource, currentUser);

        if (!missionRepository.findActiveMissionsByResourceId(id).isEmpty()) {
            throw new IllegalStateException("Không thể xóa xe đang có nhiệm vụ");
        }

        repository.delete(resource);
    }

    private void setRelationships(DispatchResource resource, DispatchResourceRequest request, Integer currentUserId) {
        User currentUser = requireCurrentUser(currentUserId);

        if (request.resourceTypeId() != null) {
            ServiceType serviceType = entityManager.find(ServiceType.class, request.resourceTypeId());
            if (serviceType == null) {
                throw new ResourceNotFoundException("Không tìm thấy loại dịch vụ với ID: " + request.resourceTypeId());
            }
            resource.setResourceType(serviceType);
        } else {
            resource.setResourceType(null);
        }



        if (hasRole(currentUser, "PROVIDER_ADMIN")) {
            if (currentUser.getProvider() == null) {
                throw new AccessDeniedException(
                        "Tài khoản Provider Admin chưa được gắn nhà cung cấp"
                );
            }
            resource.setProvider(currentUser.getProvider());
        } else if (request.providerId() != null) {
            Provider provider = entityManager.find(Provider.class, request.providerId());
            if (provider == null) {
                throw new ResourceNotFoundException("Không tìm thấy nhà xe/đơn vị với ID: " + request.providerId());
            }
            resource.setProvider(provider);
        } else {
            throw new IllegalArgumentException("Nhà cung cấp là bắt buộc");
        }

        if (request.currentDriverId() != null) {
            User driver = entityManager.find(
                    User.class,
                    request.currentDriverId()
            );

            if (driver == null) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy tài xế với ID: "
                                + request.currentDriverId()
                );
            }

            boolean isDriver = driver.getRoles()
                    .stream()
                    .anyMatch(role ->
                            "DRIVER".equalsIgnoreCase(role.getName())
                    );

            if (!isDriver) {
                throw new IllegalArgumentException(
                        "Người dùng được gán phải có vai trò DRIVER"
                );
            }

            if (resource.getProvider() == null) {
                throw new IllegalArgumentException(
                        "Xe phải thuộc một nhà cung cấp trước khi gán tài xế"
                );
            }

            if (driver.getProvider() == null ||
                    !driver.getProvider().getId()
                            .equals(resource.getProvider().getId())) {

                throw new IllegalArgumentException(
                        "Tài xế phải thuộc cùng nhà cung cấp với xe"
                );
            }

            boolean alreadyAssigned;

            if (resource.getId() == null) {
                alreadyAssigned =
                        repository.existsByCurrentDriverId(driver.getId());
            } else {
                alreadyAssigned =
                        repository.existsByCurrentDriverIdAndIdNot(
                                driver.getId(),
                                resource.getId()
                        );
            }

            if (alreadyAssigned) {
                throw new IllegalArgumentException(
                        "Tài xế này đang được gán cho một xe khác"
                );
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

    // Cập nhật vị trí theo resourceId

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

    // DRIVER
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
        ResourceLocationLog locationLog =
        new ResourceLocationLog();

        locationLog.setResource(resource);
        locationLog.setLocation(point);
        locationLog.setRecordedAt(LocalDateTime.now());

        locationLogRepository.save(locationLog);

        Integer resourceId = resource.getId();

        Integer missionId =
                missionRepository.findActiveMissionsByDriverId(driverId)
                        .stream()
                        .findFirst()
                        .map(DispatchMission::getId)
                        .orElse(null);

        double longitude = request.longitude();
        double latitude = request.latitude();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        positionPublisher.publish(
                                resourceId,
                                missionId,
                                "GPS",
                                longitude,
                                latitude
                        );
                    }
                }
        );
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
    private User requireCurrentUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy người dùng")
                );
    }
    private void assertProviderOwnership(
        DispatchResource resource,
        User currentUser
    ) {
        if (!hasRole(currentUser, "PROVIDER_ADMIN")) {
            return;
        }

        if (currentUser.getProvider() == null ||
            resource.getProvider() == null ||
            !currentUser.getProvider().getId()
                    .equals(resource.getProvider().getId())) {

            throw new AccessDeniedException(
                    "Không có quyền thao tác xe của nhà cung cấp khác"
            );
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }
}
