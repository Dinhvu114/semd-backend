package com.semd.backend.controller;

import com.semd.backend.dto.*;
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

    @PostMapping("/send-otp")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Gửi mã OTP xác thực số điện thoại", description = "Gửi OTP qua kênh cấu hình. Chế độ mock trả mã trong data; chế độ sms luôn trả data = null.")
    public ResponseEntity<BaseResponse<String>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String otpCode = authService.generateAndSendOtp(request.phoneNumber());
        return ResponseEntity.ok(new BaseResponse<>(
                200,
                true,
                otpCode != null ? "Mã OTP thử nghiệm đã được tạo" : "Mã OTP đã được gửi",
                otpCode,
                null
        ));
    }

    @PostMapping("/verify-otp")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Xác minh OTP", description = "Xác minh OTP và trả token đăng ký ngắn hạn, dùng một lần.")
    public ResponseEntity<BaseResponse<OtpVerificationResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        OtpVerificationResponse response = authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đăng ký tài khoản tự do", description = "Đăng ký REPORTER bằng verificationToken nhận từ API /auth/verify-otp.")
    public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new BaseResponse<>(200, true, "Đăng ký tài khoản thành công", null, null));
    }

    @PostMapping("/forgot-password")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Quên mật khẩu", description = "Yêu cầu khôi phục mật khẩu qua Email hoặc Số điện thoại. Hệ thống sẽ sinh và gửi OTP mô phỏng.")
    public ResponseEntity<BaseResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String otpCode = authService.forgotPassword(request);
        return ResponseEntity.ok(new BaseResponse<>(200, true, "Mã xác thực OTP đặt lại mật khẩu đã được gửi", otpCode, null));
    }

    @PostMapping("/reset-password")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đặt lại mật khẩu", description = "Sử dụng OTP nhận được để xác nhận đặt lại mật khẩu mới.")
    public ResponseEntity<BaseResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new BaseResponse<>(200, true, "Đặt lại mật khẩu thành công", null, null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đổi mật khẩu", description = "Đổi mật khẩu của người dùng hiện đang đăng nhập (yêu cầu token JWT).")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.semd.backend.security.UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(new BaseResponse<>(200, true, "Đổi mật khẩu thành công", null, null));
    }
}
