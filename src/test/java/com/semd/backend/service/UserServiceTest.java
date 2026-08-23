package com.semd.backend.service;

import com.semd.backend.dto.AdminCreateUserRequest;
import com.semd.backend.dto.UserDto;
import com.semd.backend.entity.Provider;
import com.semd.backend.entity.Role;
import com.semd.backend.entity.RoleCode;
import com.semd.backend.entity.User;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.DispatchMissionRepository;
import com.semd.backend.repository.DispatchResourceRepository;
import com.semd.backend.repository.ProviderRepository;
import com.semd.backend.repository.RoleRepository;
import com.semd.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private ProviderRepository providerRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;
    private DispatchResourceRepository dispatchResourceRepository;
    private DispatchMissionRepository dispatchMissionRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        providerRepository = mock(ProviderRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(userRepository, passwordEncoder, roleRepository, providerRepository, dispatchResourceRepository, dispatchMissionRepository);

        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1);
            return user;
        });
    }

    @Test
    void createsDispatcherWithoutProvider() {
        when(roleRepository.findByName("DISPATCHER")).thenReturn(Optional.of(new Role("DISPATCHER")));

        UserDto result = service.createInternalUser(request(RoleCode.DISPATCHER, null));

        assertEquals(java.util.Set.of("DISPATCHER"), result.roles());
        assertNull(result.providerId());
        verify(providerRepository, never()).findById(any());
    }

    @Test
    void createsDriverForExistingProvider() {
        Provider provider = new Provider();
        provider.setId(10);
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName("DRIVER")).thenReturn(Optional.of(new Role("DRIVER")));

        UserDto result = service.createInternalUser(request(RoleCode.DRIVER, 10));

        assertEquals(10, result.providerId());
    }

    @Test
    void rejectsProviderRoleWithoutProvider() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createInternalUser(request(RoleCode.PROVIDER_ADMIN, null)));

        assertTrue(error.getMessage().contains("bắt buộc"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsDispatcherForExistingProvider() {
        Provider provider = new Provider();
        provider.setId(10);
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));
        when(roleRepository.findByName("DISPATCHER")).thenReturn(Optional.of(new Role("DISPATCHER")));

        UserDto result = service.createInternalUser(request(RoleCode.DISPATCHER, 10));

        assertEquals(10, result.providerId());
    }

    @Test
    void rejectsAdminAndReporter() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createInternalUser(request(RoleCode.ADMIN, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createInternalUser(request(RoleCode.REPORTER, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsMissingProvider() {
        when(providerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createInternalUser(request(RoleCode.DRIVER, 99)));
    }

    private AdminCreateUserRequest request(RoleCode role, Integer providerId) {
        return new AdminCreateUserRequest(
                "internal01", "Secret123!", "Internal User",
                "0901000001", "internal01@ems.local", role, providerId);
    }
}
