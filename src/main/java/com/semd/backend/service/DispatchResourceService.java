package com.semd.backend.service;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.dto.request.UpdateResourceLocationRequest;
import com.semd.backend.dto.response.DispatchResourceResponse;
import com.semd.backend.entity.*;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.AmbulanceSimulationRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.ResourceLocationLogRepository;
import com.semd.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DispatchResourceService {

    private static final Logger log = LoggerFactory.getLogger(DispatchResourceService.class);

    private final DispatchResourceRepository repository;
    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final AmbulanceSimulationRepository simulationRepository;
    private final ResourceLocationLogRepository locationLogRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public DispatchResourceService(
            DispatchResourceRepository repository,
            EntityManager entityManager,
            UserRepository userRepository,
            AmbulanceSimulationRepository simulationRepository,
            ResourceLocationLogRepository locationLogRepository) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
        this.simulationRepository = simulationRepository;
        this.locationLogRepository = locationLogRepository;
    }

    // ══════════════════════════════════════════════════════
    // HELPER — phân quyền theo Provider
    // ══════════════════════════════════════════════════════
    /**
     * Trả về providerId nếu user hiện tại là chủ 1 provider (PROVIDER_ADMIN),
     * trả về null nếu user không gắn với provider nào (ADMIN xem toàn bộ).
     */
    private Integer resolveScopedProviderId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng id: " + userId));
        return user.getProvider() != null ? user.getProvider().getId() : null;
    }

    private void assertOwnsResource(DispatchResource resource, Integer userId) {
        Integer scopedProviderId = resolveScopedProviderId(userId);
        if (scopedProviderId == null) {
            return; // ADMIN — không giới hạn
        }
        Integer resourceProviderId = resource.getProvider() != null ? resource.getProvider().getId() : null;
        if (!scopedProviderId.equals(resourceProviderId)) {
            throw new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + resource.getId());
        }
    }

    // ══════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════
    @Transactional
    public DispatchResourceDto createResource(DispatchResourceRequest request, Integer userId) {
        if (repository.existsByResourceCode(request.resourceCode())) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã tồn tại");
        }

        DispatchResource resource = new DispatchResource();
        resource.setResourceCode(request.resourceCode());
        resource.setStatus(request.status());
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        setRelationships(resource, request);

        // Nếu PROVIDER_ADMIN tạo xe, ép provider của xe = provider của chính mình
        Integer scopedProviderId = resolveScopedProviderId(userId);
        if (scopedProviderId != null) {
            Provider ownProvider = entityManager.find(Provider.class, scopedProviderId);
            resource.setProvider(ownProvider);
        }

        DispatchResource saved = repository.save(resource);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<DispatchResourceDto> search(
            String keyword,
            DispatchResourceStatus status,
            Integer serviceTypeId,
            Integer providerId,
            Integer userId,
            Pageable pageable) {

        Integer scopedProviderId = resolveScopedProviderId(userId);
        // Nếu là PROVIDER_ADMIN, ép filter về đúng provider của họ (bỏ qua providerId truyền vào nếu khác)
        Integer effectiveProviderId = scopedProviderId != null ? scopedProviderId : providerId;

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
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("provider").get("id"), effectiveProviderId));
        }

        return repository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public DispatchResourceDto getResourceById(Integer id, Integer userId) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));
        assertOwnsResource(resource, userId);
        return mapToDto(resource);
    }

    @Transactional
    public DispatchResourceDto updateResource(Integer id, DispatchResourceRequest request, Integer userId) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));
        assertOwnsResource(resource, userId);

        if (repository.existsByResourceCodeAndIdNot(request.resourceCode(), id)) {
            throw new IllegalArgumentException("Mã xe cứu thương '" + request.resourceCode() + "' đã được sử dụng bởi xe khác");
        }

        resource.setResourceCode(request.resourceCode());
        resource.setStatus(request.status());
        resource.setExtendedAttributes(request.extendedAttributes());
        resource.setUpdatedAt(LocalDateTime.now());

        setRelationships(resource, request);

        // PROVIDER_ADMIN không được đổi xe sang provider khác
        Integer scopedProviderId = resolveScopedProviderId(userId);
        if (scopedProviderId != null) {
            Provider ownProvider = entityManager.find(Provider.class, scopedProviderId);
            resource.setProvider(ownProvider);
        }

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public DispatchResourceDto updateResourceStatus(Integer id, DispatchResourceStatus status, Integer userId) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));
        assertOwnsResource(resource, userId);

        resource.setStatus(status);
        resource.setUpdatedAt(LocalDateTime.now());

        DispatchResource updated = repository.save(resource);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteResource(Integer id, Integer userId) {
        DispatchResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe cứu thương với ID: " + id));
        assertOwnsResource(resource, userId);
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

    // ══════════════════════════════════════════════════════
    // DRIVER / DISPATCHER — cập nhật vị trí
    // ══════════════════════════════════════════════════════

    /**
     * Dispatcher cập nhật thủ công vị trí xe.
     * Nếu xe đang có simulation active thì simulation là nguồn currentLocation,
     * lần cập nhật này chỉ được ghi vào lịch sử, không ghi đè currentLocation.
     */
    @Transactional
    public void updateLocation(
            Integer resourceId,
            UpdateResourceLocationRequest request
    ) {
        DispatchResource resource =
                repository.findById(resourceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy phương tiện với id = " + resourceId
                                )
                        );

        Point point = geometryFactory.createPoint(
                new Coordinate(request.longitude(), request.latitude())
        );
        point.setSRID(4326);

        boolean hasActiveSimulation = hasActiveSimulation(resourceId);

        saveLocationLog(resource, point, "MANUAL");

        if (hasActiveSimulation) {
            log.info("Bỏ qua cập nhật currentLocation cho resource {} vì simulation đang active.",
                    resourceId);
            return;
        }

        resource.setCurrentLocation(point);
        resource.setUpdatedAt(LocalDateTime.now());
        repository.save(resource);
    }

    /**
     * Driver tự cập nhật vị trí GPS từ mobile.
     * Khi simulation đang active (READY/RUNNING/STOPPED), simulation là nguồn
     * ghi currentLocation duy nhất — GPS mobile vẫn được lưu lịch sử nhưng
     * không ghi đè currentLocation để tránh xung đột với tick.
     * Không dùng pessimistic lock ở đây để tránh deadlock với transaction tick().
     */
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
                new Coordinate(request.longitude(), request.latitude())
        );
        point.setSRID(4326);

        boolean hasActiveSimulation = hasActiveSimulation(resource.getId());

        saveLocationLog(resource, point, "REAL_GPS");

        if (hasActiveSimulation) {
            log.info("Bỏ qua cập nhật currentLocation cho resource {} vì simulation đang active. "
                    + "GPS mobile vẫn được lưu lịch sử.", resource.getId());
            return;
        }

        resource.setCurrentLocation(point);
        resource.setUpdatedAt(LocalDateTime.now());
        repository.save(resource);
    }

    private boolean hasActiveSimulation(Integer resourceId) {
        return simulationRepository.findByResourceIdAndStatusIn(
                resourceId,
                List.of(SimulationStatus.READY, SimulationStatus.RUNNING, SimulationStatus.STOPPED)
        ).isPresent();
    }

    private void saveLocationLog(DispatchResource resource, Point point, String sourceType) {
        ResourceLocationLog locationLog = new ResourceLocationLog();
        locationLog.setResource(resource);
        locationLog.setSourceType(sourceType);
        locationLog.setLocation(point);
        locationLog.setRecordedAt(LocalDateTime.now());
        locationLogRepository.save(locationLog);
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