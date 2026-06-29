package com.semd.backend.controller;

import com.semd.backend.dto.UserDto;
import com.semd.backend.dto.UserRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Quản lý danh sách người dùng (nhân viên điều phối, tài xế, chủ xe...)")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới người dùng", description = "Tạo mới tài khoản người dùng với các thông tin chi tiết và vai trò")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody UserRequest request) {
        UserDto result = service.createUser(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả người dùng", description = "Trả về danh sách toàn bộ người dùng trong hệ thống")
    public ResponseEntity<BaseResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> result = service.getAllUsers();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Trả về thông tin chi tiết của tài khoản đang đăng nhập dựa trên token")
    public ResponseEntity<BaseResponse<UserDto>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.semd.backend.security.UserPrincipal principal) {
        if (principal == null) {
            throw new com.semd.backend.exception.AuthException("Chưa đăng nhập hoặc token không hợp lệ");
        }
        UserDto result = service.getUserById(principal.getId());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy chi tiết tài khoản người dùng theo ID")
    public ResponseEntity<BaseResponse<UserDto>> getUserById(@PathVariable Integer id) {
        UserDto result = service.getUserById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin chi tiết của người dùng theo ID")
    public ResponseEntity<BaseResponse<UserDto>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserRequest request) {
        UserDto result = service.updateUser(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa người dùng", description = "Xóa tài khoản người dùng khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable Integer id) {
        service.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
