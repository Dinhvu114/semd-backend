package com.semd.backend.controller;

import com.semd.backend.dto.DispatchResourceDto;
import com.semd.backend.dto.DispatchResourceRequest;
import com.semd.backend.dto.ResourceStatusUpdateRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.DispatchResourceService;
import com.semd.backend.entity.DispatchResourceStatus;
import com.semd.backend.dto.common.Metadata;
import com.semd.backend.dto.common.PageRequestDto;
import com.semd.backend.dto.request.UpdateResourceLocationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springdoc.core.annotations.ParameterObject;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_ADMIN')")
    @Operation(summary = "Thêm mới xe cứu thương", description = "Tạo mới tài nguyên xe cứu thương / đội ngũ y tế")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> createResource(@Valid @RequestBody DispatchResourceRequest request) {
        DispatchResourceDto result = service.createResource(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PROVIDER_ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả xe cứu thương", description = "Trả về danh sách toàn bộ các xe cứu thương đang quản lý")
    public ResponseEntity<BaseResponse<List<DispatchResourceDto>>> getAllResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DispatchResourceStatus status,
            @RequestParam(required = false) Integer serviceTypeId,
            @RequestParam(required = false) Integer providerId,
            @RequestParam(required = false) Integer zoneId,
            @ParameterObject @ModelAttribute PageRequestDto pagination) {
        Page<DispatchResourceDto> result = service.search(
                keyword, status, serviceTypeId, providerId, zoneId,
                pagination.toPageable(Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return ResponseEntity.ok(BaseResponse.success(result.getContent(), Metadata.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PROVIDER_ADMIN')")
    @Operation(summary = "Lấy thông tin chi tiết xe cứu thương", description = "Lấy chi tiết thông tin xe cứu thương theo ID")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> getResourceById(@PathVariable Integer id) {
        DispatchResourceDto result = service.getResourceById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_ADMIN')")
    @Operation(summary = "Cập nhật thông tin xe cứu thương", description = "Cập nhật thông tin chi tiết xe cứu thương theo ID")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> updateResource(
            @PathVariable Integer id,
            @Valid @RequestBody DispatchResourceRequest request) {
        DispatchResourceDto result = service.updateResource(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER_ADMIN', 'DISPATCHER')")
    @Operation(summary = "Thay đổi trạng thái hoạt động của xe", description = "Cập nhật nhanh trạng thái hoạt động (ví dụ: AVAILABLE, BUSY, MAINTENANCE...)")
    public ResponseEntity<BaseResponse<DispatchResourceDto>> updateResourceStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ResourceStatusUpdateRequest request) {
        DispatchResourceDto result = service.updateResourceStatus(id, request.status());
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa xe cứu thương", description = "Xóa xe cứu thương khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteResource(@PathVariable Integer id) {
        service.deleteResource(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('DISPATCHER')")
    @Operation(summary = "Cập nhật vị trí xe cứu thương")
    public ResponseEntity<BaseResponse<Void>> updateLocation(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateResourceLocationRequest request
    ) {

        service.updateLocation(id, request);

        return ResponseEntity.ok(
                BaseResponse.success(
                        200,
                        "Cập nhật vị trí phương tiện thành công",
                        null
                )
        );
    }
}
