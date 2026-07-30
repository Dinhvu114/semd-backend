package com.semd.backend.controller;

import com.semd.backend.dto.AdminCreateUserRequest;
import com.semd.backend.dto.UserDto;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin users", description = "Quản lý tài khoản nội bộ")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Tạo tài khoản nội bộ",
            description = "Admin tạo DISPATCHER (có thể thuộc Provider hoặc không), PROVIDER_ADMIN hoặc DRIVER")
    public ResponseEntity<BaseResponse<UserDto>> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return new ResponseEntity<>(
                BaseResponse.success(userService.createInternalUser(request)),
                HttpStatus.CREATED);
    }
}
