package com.semd.backend.controller;

import com.semd.backend.dto.EdgeNodeDto;
import com.semd.backend.dto.EdgeNodeRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.EdgeNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/edge-nodes")
@Tag(name = "Edge Nodes", description = "Quản lý vùng quản lý điều phối địa lý (Edge Nodes)")
public class EdgeNodeController {

    private final EdgeNodeService service;

    public EdgeNodeController(EdgeNodeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới vùng quản lý", description = "Tạo mới một vùng quản lý với danh sách các tọa độ của Polygon phủ sóng")
    public ResponseEntity<BaseResponse<EdgeNodeDto>> createEdgeNode(@Valid @RequestBody EdgeNodeRequest request) {
        EdgeNodeDto result = service.createEdgeNode(request);
        return new ResponseEntity<>(BaseResponse.success(result), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy toàn bộ vùng quản lý", description = "Lấy danh sách toàn bộ các vùng quản lý hoạt động trong hệ thống")
    public ResponseEntity<BaseResponse<List<EdgeNodeDto>>> getAllEdgeNodes() {
        List<EdgeNodeDto> result = service.getAllEdgeNodes();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy vùng quản lý theo ID", description = "Lấy thông tin chi tiết một vùng quản lý theo ID")
    public ResponseEntity<BaseResponse<EdgeNodeDto>> getEdgeNodeById(@PathVariable Integer id) {
        EdgeNodeDto result = service.getEdgeNodeById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật vùng quản lý", description = "Cập nhật thông tin chi tiết (tên, vùng đa giác phủ sóng) của vùng quản lý theo ID")
    public ResponseEntity<BaseResponse<EdgeNodeDto>> updateEdgeNode(
            @PathVariable Integer id,
            @Valid @RequestBody EdgeNodeRequest request) {
        EdgeNodeDto result = service.updateEdgeNode(id, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa vùng quản lý", description = "Xóa một vùng quản lý ra khỏi hệ thống theo ID")
    public ResponseEntity<BaseResponse<Void>> deleteEdgeNode(@PathVariable Integer id) {
        service.deleteEdgeNode(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
