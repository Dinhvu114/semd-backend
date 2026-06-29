package com.semd.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.semd.backend.dto.AuthResponse;
import com.semd.backend.dto.LoginRequest;
import com.semd.backend.dto.TokenRefreshRequest;
import com.semd.backend.entity.User;
import com.semd.backend.exception.AuthException;
import com.semd.backend.repository.UserRepository;
import com.semd.backend.util.JwtUtil;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByUsername(request.username())
                .orElseThrow(() -> new AuthException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AuthException("Tài khoản của bạn đã bị khóa");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Lưu refresh token vào Redis
        refreshTokenService.storeRefreshToken(user.getUsername(), refreshToken, jwtUtil.getRefreshTtlSeconds());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtUtil.getAccessTtlSeconds(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoles().stream().map(com.semd.backend.entity.Role::getName).collect(java.util.stream.Collectors.toSet())
        );
    }

    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // 1. Kiểm tra tính hợp lệ và hết hạn của token
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new AuthException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String username = jwtUtil.extractUsername(refreshToken);

        // 2. Kiểm tra sự tồn tại trong Redis
        if (!refreshTokenService.exists(username, refreshToken)) {
            throw new AuthException("Refresh token không tồn tại hoặc đã bị thu hồi");
        }

        // 3. Tìm user và kiểm tra hoạt động
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AuthException("Người dùng không tồn tại"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AuthException("Tài khoản của bạn đã bị khóa");
        }

        // 4. Tạo cặp token mới (Refresh Token Rotation - tăng độ bảo mật)
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        // 5. Thu hồi token cũ và lưu token mới vào Redis
        refreshTokenService.revoke(username, refreshToken);
        refreshTokenService.storeRefreshToken(username, newRefreshToken, jwtUtil.getRefreshTtlSeconds());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtUtil.getAccessTtlSeconds(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoles().stream().map(com.semd.backend.entity.Role::getName).collect(java.util.stream.Collectors.toSet())
        );
    }

    public void logout(String refreshToken) {
        try {
            if (jwtUtil.isTokenValid(refreshToken) && jwtUtil.isRefreshToken(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);
                refreshTokenService.revoke(username, refreshToken);
            }
        } catch (Exception e) {
            // Bỏ qua lỗi phân tích token khi đăng xuất
        }
    }
}
