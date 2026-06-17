package com.semd.backend.controller;

import com.semd.backend.dto.ServiceTypeDto;
import com.semd.backend.dto.ServiceTypeRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.ServiceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service-types")
@Tag(name = "Service Types", description = "Quản lý danh mục loại dịch vụ / loại xe cấp cứu")
public class ServiceTypeController {

    private final ServiceTypeService service;

    public ServiceTypeController(ServiceTypeService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Tạo mới loại dịch vụ", description = "Tạo danh mục loại dịch vụ/xe cấp cứu mới")
    public ResponseEntity<BaseResponse<ServiceTypeDto>> createServiceType(@Valid @RequestBody ServiceTypeRequest request) {
        ServiceTypeDto result = service.createServiceType(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả loại dịch vụ", description = "Trả về danh sách toàn bộ các loại dịch vụ")
    public ResponseEntity<BaseResponse<List<ServiceTypeDto>>> getAllServiceTypes() {
        List<ServiceTypeDto> result = service.getAllServiceTypes();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy loại dịch vụ theo ID", description = "Lấy chi tiết loại dịch vụ theo ID")
    public ResponseEntity<BaseResponse<ServiceTypeDto>> getServiceTypeById(@PathVariable Integer id) {
        ServiceTypeDto result = service.getServiceTypeById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật loại dịch vụ", description = "Cập nhật thông tin chi tiết loại dịch vụ theo ID")
    public ResponseEntity<BaseResponse<ServiceTypeDto>> updateServiceType(
            @PathVariable Integer id,
            @Valid @RequestBody ServiceTypeRequest request) {
        ServiceTypeDto result = service.updateServiceType(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa loại dịch vụ", description = "Xóa loại dịch vụ ra khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteServiceType(@PathVariable Integer id) {
        service.deleteServiceType(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
