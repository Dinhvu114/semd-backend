package com.semd.backend.service;

import com.semd.backend.dto.ProviderDto;
import com.semd.backend.dto.ProviderRequest;
import com.semd.backend.entity.Provider;
import com.semd.backend.entity.User;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.ProviderRepository;
import com.semd.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderService {

    private final DispatchResourceRepository dispatchResourceRepository;
    private final ProviderRepository repository;
    private final UserRepository userRepository;

    public ProviderService(DispatchResourceRepository dispatchResourceRepository, ProviderRepository repository, UserRepository userRepository) {
        this.dispatchResourceRepository = dispatchResourceRepository;
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProviderDto createProvider(
            ProviderRequest request
    ) {
        validateProviderRequest(request);

        if (repository.existsByProviderName(
                request.providerName()
        )) {
            throw new IllegalArgumentException(
                    "Tên đơn vị nhà xe/phòng khám '"
                            + request.providerName()
                            + "' đã tồn tại"
            );
        }

        User owner = userRepository
                .findById(request.ownerUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy chủ sở hữu với ID: "
                                        + request.ownerUserId()
                        )
                );

        // QUAN TRỌNG
        validateProviderOwner(owner);
        validateOwnerAssignment(owner, null);

        Provider provider = new Provider();

        provider.setOwner(owner);
        provider.setProviderName(
                request.providerName()
        );
        provider.setProviderType(
                request.providerType()
        );
        provider.setBusinessLicense(
                request.businessLicense()
        );
        provider.setContactPhone(
                request.contactPhone()
        );
        provider.setContactAddress(
                request.contactAddress()
        );
        provider.setCommissionRate(
                request.commissionRate() != null
                        ? request.commissionRate()
                        : BigDecimal.ZERO
        );
        provider.setIsVerified(
                request.isVerified() != null
                        ? request.isVerified()
                        : false
        );
        provider.setIsActive(
                request.isActive() != null
                        ? request.isActive()
                        : true
        );
        provider.setCreatedAt(
                LocalDateTime.now()
        );

        // 1. Provider phải có ID trước
        Provider saved =
                repository.save(provider);

        // 2. Đồng bộ chiều User -> Provider
        owner.setProvider(saved);
        userRepository.save(owner);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderDto> getAllProviders() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProviderDto getProviderById(Integer id) {
        Provider provider = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị với ID: " + id));
        return mapToDto(provider);
    }

    @Transactional
    public ProviderDto updateProvider(
            Integer id,
            ProviderRequest request
    ) {
        validateProviderRequest(request);

        Provider provider =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy đơn vị với ID: "
                                                + id
                                )
                        );

        if (repository.existsByProviderNameAndIdNot(
                request.providerName(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Tên đơn vị nhà xe/phòng khám '"
                            + request.providerName()
                            + "' đã được sử dụng bởi đơn vị khác"
            );
        }

        User newOwner = userRepository
                .findById(request.ownerUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy chủ sở hữu với ID: "
                                        + request.ownerUserId()
                        )
                );

        validateProviderOwner(newOwner);
        validateOwnerAssignment(newOwner, id);

        User oldOwner = provider.getOwner();
            // Nếu đổi sang Owner khác
        if (oldOwner != null
                && !oldOwner.getId()
                        .equals(newOwner.getId())) {

            // Chỉ bỏ provider nếu oldOwner đang thực sự
            // trỏ vào Provider hiện tại
            if (oldOwner.getProvider() != null
                    && id.equals(
                            oldOwner.getProvider().getId()
                    )) {

                oldOwner.setProvider(null);
                userRepository.save(oldOwner);
            }
        }

        provider.setOwner(newOwner);
        provider.setProviderName(request.providerName());
        provider.setProviderType(request.providerType());
        provider.setBusinessLicense(request.businessLicense());
        provider.setContactPhone(request.contactPhone());
        provider.setContactAddress(request.contactAddress());
        if (request.commissionRate() != null) {
            provider.setCommissionRate(request.commissionRate());
        }
        if (request.isVerified() != null) {
            provider.setIsVerified(request.isVerified());
        }
        if (request.isActive() != null) {
            provider.setIsActive(request.isActive());
        }

        Provider updated = repository.save(provider);
        newOwner.setProvider(updated);
        userRepository.save(newOwner);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteProvider(Integer id) {
        Provider provider =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy đơn vị với ID: "
                                                + id
                                )
                        );
        if (dispatchResourceRepository.existsByProviderId(id)) {
            throw new IllegalStateException(
                    "Không thể xóa nhà cung cấp đang còn xe cứu thương"
            );
        }
        User owner = provider.getOwner();

        if (owner != null
                && owner.getProvider() != null
                && id.equals(
                        owner.getProvider().getId()
                )) {

            owner.setProvider(null);
            userRepository.save(owner);
        }

        repository.delete(provider);
    }

    private ProviderDto mapToDto(Provider provider) {
        Integer ownerUserId = provider.getOwner() != null ? provider.getOwner().getId() : null;
        String ownerUsername = provider.getOwner() != null ? provider.getOwner().getUsername() : null;
        String ownerFullName = provider.getOwner() != null ? provider.getOwner().getFullName() : null;

        return new ProviderDto(
                provider.getId(),
                ownerUserId,
                ownerUsername,
                ownerFullName,
                provider.getProviderName(),
                provider.getProviderType(),
                provider.getBusinessLicense(),
                provider.getContactPhone(),
                provider.getContactAddress(),
                provider.getCommissionRate(),
                provider.getIsVerified(),
                provider.getIsActive(),
                provider.getCreatedAt()
        );
    }

    private void validateProviderRequest(ProviderRequest request) {
        if (!"TRANSPORT".equals(request.providerType()) && !"CLINIC".equals(request.providerType())) {
            throw new IllegalArgumentException("Loại đơn vị (providerType) chỉ được phép là 'TRANSPORT' hoặc 'CLINIC'");
        }
    }

    private void validateProviderOwner(User owner) {
        boolean isProviderAdmin = owner.getRoles()
                .stream()
                .anyMatch(role ->
                        "PROVIDER_ADMIN".equalsIgnoreCase(role.getName())
                );

        if (!isProviderAdmin) {
            throw new IllegalArgumentException(
                    "Chủ sở hữu Provider phải có vai trò PROVIDER_ADMIN"
            );
        }
    }
    private void validateOwnerAssignment(
        User owner,
        Integer currentProviderId
    ) {
        if (owner.getProvider() == null) {
            return;
        }

        // Khi update chính Provider hiện tại thì vẫn hợp lệ
        if (currentProviderId != null
                && currentProviderId.equals(
                        owner.getProvider().getId()
                )) {
            return;
        }

        throw new IllegalArgumentException(
                "Tài khoản Provider Admin này đã thuộc một Provider khác"
        );
    }
}
