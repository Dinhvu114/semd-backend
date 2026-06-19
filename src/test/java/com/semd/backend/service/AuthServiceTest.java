package com.semd.backend.service;

import com.semd.backend.dto.AuthResponse;
import com.semd.backend.dto.LoginRequest;
import com.semd.backend.dto.TokenRefreshRequest;
import com.semd.backend.entity.User;
import com.semd.backend.exception.AuthException;
import com.semd.backend.repository.UserRepository;
import com.semd.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepo;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtil;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authService = new AuthService(userRepo, encoder, jwtUtil, refreshTokenService);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashed_password");
        testUser.setFullName("Test User");
        testUser.setRole("USER");
        testUser.setIsActive(true);
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("testuser", "123456");
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("123456", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("refresh_token");
        when(jwtUtil.getRefreshTtlSeconds()).thenReturn(604800L);
        when(jwtUtil.getAccessTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.refreshToken());
        assertEquals(1, response.userId());
        assertEquals("testuser", response.username());
        
        verify(refreshTokenService, times(1)).storeRefreshToken("testuser", "refresh_token", 604800L);
    }

    @Test
    void testLogin_UserNotFound() {
        LoginRequest request = new LoginRequest("wronguser", "123456");
        when(userRepo.findByUsername("wronguser")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác", exception.getMessage());
    }

    @Test
    void testLogin_WrongPassword() {
        LoginRequest request = new LoginRequest("testuser", "654321");
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("654321", "hashed_password")).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác", exception.getMessage());
    }

    @Test
    void testLogin_UserInactive() {
        testUser.setIsActive(false);
        LoginRequest request = new LoginRequest("testuser", "123456");
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encoder.matches("123456", "hashed_password")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("Tài khoản của bạn đã bị khóa", exception.getMessage());
    }

    @Test
    void testRefreshToken_Success() {
        TokenRefreshRequest request = new TokenRefreshRequest("old_refresh_token");
        when(jwtUtil.isTokenValid("old_refresh_token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("old_refresh_token")).thenReturn(true);
        when(jwtUtil.extractUsername("old_refresh_token")).thenReturn("testuser");
        when(refreshTokenService.exists("testuser", "old_refresh_token")).thenReturn(true);
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("new_access_token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("new_refresh_token");
        when(jwtUtil.getRefreshTtlSeconds()).thenReturn(604800L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertEquals("new_refresh_token", response.refreshToken());
        
        verify(refreshTokenService, times(1)).revoke("testuser", "old_refresh_token");
        verify(refreshTokenService, times(1)).storeRefreshToken("testuser", "new_refresh_token", 604800L);
    }

    @Test
    void testLogout_Success() {
        String refreshToken = "refresh_token_to_revoke";
        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("testuser");

        authService.logout(refreshToken);

        verify(refreshTokenService, times(1)).revoke("testuser", refreshToken);
    }
}
