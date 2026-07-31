package com.semd.backend.controller;

import com.semd.backend.dto.ProviderDto;
import com.semd.backend.dto.ProviderRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Quản lý đơn vị cung cấp xe cứu thương / phòng khám (Providers)")
public class ProviderController {

    private final ProviderService service;

    public ProviderController(ProviderService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_ADMIN')")
    @Operation(summary = "Tạo mới đơn vị xe/phòng khám", description = "Đăng ký một đơn vị cung cấp tài nguyên mới liên kết với một người dùng (Owner)")
    public ResponseEntity<BaseResponse<ProviderDto>> createProvider(@Valid @RequestBody ProviderRequest request) {
        ProviderDto result = service.createProvider(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Lấy tất cả các đơn vị cung cấp", description = "Trả về danh sách toàn bộ các nhà cung cấp hoạt động trong hệ thống")
    public ResponseEntity<BaseResponse<List<ProviderDto>>> getAllProviders() {
        List<ProviderDto> result = service.getAllProviders();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PROVIDER_ADMIN')")
    @Operation(summary = "Lấy đơn vị cung cấp theo ID", description = "Chi tiết một đơn vị cung cấp theo ID")
    public ResponseEntity<BaseResponse<ProviderDto>> getProviderById(@PathVariable Integer id) {
        ProviderDto result = service.getProviderById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_ADMIN')")
    @Operation(summary = "Cập nhật thông tin đơn vị cung cấp", description = "Cập nhật thông tin chi tiết của đơn vị cung cấp theo ID")
    public ResponseEntity<BaseResponse<ProviderDto>> updateProvider(
            @PathVariable Integer id,
            @Valid @RequestBody ProviderRequest request) {
        ProviderDto result = service.updateProvider(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa đơn vị cung cấp", description = "Xóa một đơn vị cung cấp khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteProvider(@PathVariable Integer id) {
        service.deleteProvider(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
