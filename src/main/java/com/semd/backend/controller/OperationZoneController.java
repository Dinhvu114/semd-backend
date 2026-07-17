package com.semd.backend.controller;

import com.semd.backend.dto.OperationZoneDto;
import com.semd.backend.dto.OperationZoneRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.OperationZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/operation-zones")
@Tag(name = "Operation Zones", description = "Quản lý vùng hoạt động điều phối địa lý (Operation Zones)")
public class OperationZoneController {

    private final OperationZoneService service;

    public OperationZoneController(OperationZoneService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới vùng quản lý", description = "Tạo mới một vùng quản lý với danh sách các tọa độ của Polygon phủ sóng")
    public ResponseEntity<BaseResponse<OperationZoneDto>> createOperationZone(@Valid @RequestBody OperationZoneRequest request) {
        OperationZoneDto result = service.createOperationZone(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy toàn bộ vùng quản lý", description = "Lấy danh sách toàn bộ các vùng quản lý hoạt động trong hệ thống")
    public ResponseEntity<BaseResponse<List<OperationZoneDto>>> getAllOperationZones() {
        List<OperationZoneDto> result = service.getAllOperationZones();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy vùng quản lý theo ID", description = "Lấy thông tin chi tiết một vùng quản lý theo ID")
    public ResponseEntity<BaseResponse<OperationZoneDto>> getOperationZoneById(@PathVariable Integer id) {
        OperationZoneDto result = service.getOperationZoneById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật vùng quản lý", description = "Cập nhật thông tin chi tiết (tên, vùng đa giác phủ sóng) của vùng quản lý theo ID")
    public ResponseEntity<BaseResponse<OperationZoneDto>> updateOperationZone(
            @PathVariable Integer id,
            @Valid @RequestBody OperationZoneRequest request) {
        OperationZoneDto result = service.updateOperationZone(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa vùng quản lý", description = "Xóa một vùng quản lý ra khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteOperationZone(@PathVariable Integer id) {
        service.deleteOperationZone(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
