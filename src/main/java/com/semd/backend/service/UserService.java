package com.semd.backend.service;

import com.semd.backend.dto.UserDto;
import com.semd.backend.dto.UserRequest;
import com.semd.backend.dto.AdminCreateUserRequest;
import com.semd.backend.entity.Provider;
import com.semd.backend.entity.RoleCode;
import com.semd.backend.entity.User;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.entity.Role;
import com.semd.backend.repository.RoleRepository;
import com.semd.backend.repository.UserRepository;
import com.semd.backend.repository.ProviderRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final ProviderRepository providerRepository;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, RoleRepository roleRepository,
                       ProviderRepository providerRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public UserDto createInternalUser(AdminCreateUserRequest request) {
        Set<RoleCode> creatableRoles = Set.of(
                RoleCode.DISPATCHER,
                RoleCode.PROVIDER_ADMIN,
                RoleCode.DRIVER);
        if (!creatableRoles.contains(request.role())) {
            throw new IllegalArgumentException(
                    "Admin chỉ được tạo tài khoản DISPATCHER, PROVIDER_ADMIN hoặc DRIVER");
        }

        if (request.role() != RoleCode.DISPATCHER && request.providerId() == null) {
            throw new IllegalArgumentException(request.role() + " bắt buộc phải thuộc một Provider");
        }

        Provider provider = null;
        if (request.providerId() != null) {
            provider = providerRepository.findById(request.providerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy Provider với ID: " + request.providerId()));
        }

        validateUniqueIdentity(request.username(), request.email(), request.phone());

        Role role = roleRepository.findByName(request.role().name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy vai trò: " + request.role().name()));

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phone());
        user.setEmail(request.email());
        user.setRoles(Set.of(role));
        user.setProvider(provider);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return mapToDto(repository.save(user));
    }

    private void validateUniqueIdentity(String username, String email, String phone) {
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại");
        }
        if (email != null && !email.isBlank() && repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã tồn tại");
        }
        if (repository.existsByPhoneNumber(phone)) {
            throw new IllegalArgumentException("Số điện thoại '" + phone + "' đã tồn tại");
        }
    }

    @Transactional
    public UserDto createUser(UserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.username() + "' đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.username());
        
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo mới người dùng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setEmail(request.email());
        Set<Role> roles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleName)))
                .collect(Collectors.toSet());
        user.setRoles(roles);
        user.setIsActive(request.isActive() != null ? request.isActive() : true);
        user.setCreatedAt(LocalDateTime.now());

        User saved = repository.save(user);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> search(String keyword, Boolean isActive, String role, Pageable pageable) {
        Specification<User> specification = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("phoneNumber")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }
        if (isActive != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        if (role != null && !role.isBlank()) {
            String normalizedRole = role.trim().toUpperCase();
            specification = specification.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(cb.upper(root.join("roles").get("name")), normalizedRole);
            });
        }
        return repository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Integer id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        if (!user.getUsername().equals(request.username()) && repository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.username() + "' đã tồn tại");
        }

        user.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setEmail(request.email());
        Set<Role> roles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleName)))
                .collect(Collectors.toSet());
        user.setRoles(roles);
        if (request.isActive() != null) {
            user.setIsActive(request.isActive());
        }

        User updated = repository.save(user);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteUser(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id);
        }
        repository.deleteById(id);
    }

    private UserDto mapToDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                roleNames,
                user.getProvider() != null ? user.getProvider().getId() : null,
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
