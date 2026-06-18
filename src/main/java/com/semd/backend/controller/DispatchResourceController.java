package com.semd.backend.controller;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.dto.ResourceStatusUpdateRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.DispatchResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-resources")
@Tag(name = "Dispatch Resources", description = "Quản lý xe cứu thương / tài nguyên điều phối")
public class DispatchResourceController {

    private final DispatchResourceService service;

    public DispatchResourceController(DispatchResourceService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Thêm mới xe cứu thương", description = "Tạo mới tài nguyên xe cứu thương / đội ngũ y tế")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> createResource(@Valid @RequestBody DispatchResourceRequest request) {
        DispatchResourceDto result = service.createResource(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả xe cứu thương", description = "Trả về danh sách toàn bộ các xe cứu thương đang quản lý")
    public ResponseEntity<BaseResponse<List<DispatchResourceDto>>> getAllResources() {
        List<DispatchResourceDto> result = service.getAllResources();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết xe cứu thương", description = "Lấy chi tiết thông tin xe cứu thương theo ID")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> getResourceById(@PathVariable Integer id) {
        DispatchResourceDto result = service.getResourceById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin xe cứu thương", description = "Cập nhật thông tin chi tiết xe cứu thương theo ID")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> updateResource(
            @PathVariable Integer id,
            @Valid @RequestBody DispatchResourceRequest request) {
        DispatchResourceDto result = service.updateResource(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái hoạt động của xe", description = "Cập nhật nhanh trạng thái hoạt động (ví dụ: AVAILABLE, BUSY, MAINTENANCE...)")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> updateResourceStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ResourceStatusUpdateRequest request) {
        DispatchResourceDto result = service.updateResourceStatus(id, request.status());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa xe cứu thương", description = "Xóa xe cứu thương khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteResource(@PathVariable Integer id) {
        service.deleteResource(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
