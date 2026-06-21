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
    @Operation(summary = "Tạo mới người dùng", description = "Tạo mới tài khoản người dùng với các thông tin chi tiết và vai trò")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody UserRequest request) {
        UserDto result = service.createUser(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả người dùng", description = "Trả về danh sách toàn bộ người dùng trong hệ thống")
    public ResponseEntity<BaseResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> result = service.getAllUsers();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy chi tiết tài khoản người dùng theo ID")
    public ResponseEntity<BaseResponse<UserDto>> getUserById(@PathVariable Integer id) {
        UserDto result = service.getUserById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin chi tiết của người dùng theo ID")
    public ResponseEntity<BaseResponse<UserDto>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserRequest request) {
        UserDto result = service.updateUser(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng", description = "Xóa tài khoản người dùng khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable Integer id) {
        service.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
