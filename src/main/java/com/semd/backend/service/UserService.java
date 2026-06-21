package com.semd.backend.service;

import com.semd.backend.dto.UserDto;
import com.semd.backend.dto.UserRequest;
import com.semd.backend.entity.User;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
        user.setRole(request.role());
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
        user.setRole(request.role());
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
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
