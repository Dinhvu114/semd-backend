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
    private final OtpService otpService;
    private final com.semd.backend.repository.RoleRepository roleRepo;

    public AuthService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            OtpService otpService,
            com.semd.backend.repository.RoleRepository roleRepo
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.roleRepo = roleRepo;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByUsernameOrEmailOrPhoneNumber(request.username(), request.username(), request.username())
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

    @org.springframework.transaction.annotation.Transactional
    public String generateAndSendOtp(String phoneNumber) {
        return otpService.generateAndSendOtp(phoneNumber);
    }

    @org.springframework.transaction.annotation.Transactional
    public void register(com.semd.backend.dto.RegisterRequest request) {
        // 1. Xác thực OTP
        if (!otpService.verifyOtp(request.phoneNumber(), request.otpCode())) {
            throw new IllegalArgumentException("Mã xác thực OTP không chính xác hoặc đã hết hạn");
        }

        // 2. Kiểm tra trùng lặp thông tin
        if (userRepo.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.username() + "' đã tồn tại");
        }
        if (userRepo.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' đã tồn tại");
        }
        if (userRepo.existsByPhoneNumber(request.phoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại '" + request.phoneNumber() + "' đã tồn tại");
        }

        // 3. Tạo user mới với vai trò mặc định REPORTER
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setEmail(request.email());
        user.setIsActive(true);
        user.setCreatedAt(java.time.LocalDateTime.now());

        com.semd.backend.entity.Role reporterRole = roleRepo.findByName("REPORTER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò REPORTER trên hệ thống."));
        user.setRoles(java.util.Set.of(reporterRole));

        userRepo.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public String forgotPassword(com.semd.backend.dto.ForgotPasswordRequest request) {
        User user = userRepo.findByUsernameOrEmailOrPhoneNumber(request.identity(), request.identity(), request.identity())
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException(
                        "Không tìm thấy tài khoản với email/số điện thoại cung cấp: " + request.identity()));

        // Sinh OTP gửi đến số điện thoại đăng ký của user
        return otpService.generateAndSendOtp(user.getPhoneNumber());
    }

    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(com.semd.backend.dto.ResetPasswordRequest request) {
        User user = userRepo.findByUsernameOrEmailOrPhoneNumber(request.identity(), request.identity(), request.identity())
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException(
                        "Không tìm thấy tài khoản với email/số điện thoại cung cấp: " + request.identity()));

        if (!otpService.verifyOtp(user.getPhoneNumber(), request.otpCode())) {
            throw new IllegalArgumentException("Mã xác thực OTP không chính xác hoặc đã hết hạn");
        }

        user.setPasswordHash(encoder.encode(request.newPassword()));
        userRepo.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void changePassword(String username, com.semd.backend.dto.ChangePasswordRequest request) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException("Người dùng không tồn tại"));

        if (!encoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }

        user.setPasswordHash(encoder.encode(request.newPassword()));
        userRepo.save(user);
    }
}
