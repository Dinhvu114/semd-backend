package com.semd.backend.controller;

import com.semd.backend.dto.AuthResponse;
import com.semd.backend.dto.LoginRequest;
import com.semd.backend.dto.TokenRefreshRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API xác thực người dùng (Đăng nhập, Đăng xuất, Làm mới token)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đăng nhập hệ thống", description = "Xác thực người dùng bằng username và password, trả về Access Token và Refresh Token")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Làm mới Access Token", description = "Sử dụng Refresh Token hợp lệ để tạo cặp Access Token và Refresh Token mới (Token Rotation)")
    public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đăng xuất", description = "Thu hồi Refresh Token trên hệ thống Redis")
    public ResponseEntity<BaseResponse<Void>> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new BaseResponse<>(200, true, "Đăng xuất thành công", null, null));
    }
}
