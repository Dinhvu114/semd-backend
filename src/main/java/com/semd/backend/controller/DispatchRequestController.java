package com.semd.backend.controller;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.service.DispatchRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-requests")
@Tag(name = "Dispatch Requests", description = "Quản lý yêu cầu điều phối cấp cứu")
public class DispatchRequestController {

    private final DispatchRequestService requestService;

    public DispatchRequestController(DispatchRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Lấy danh sách tất cả yêu cầu điều phối", 
            description = "Trả về danh sách toàn bộ các yêu cầu điều phối sắp xếp theo thời gian tạo giảm dần")
    public ResponseEntity<BaseResponse<List<DispatchRequestDto>>> getAllRequests() {
        List<DispatchRequestDto> result = requestService.getAllRequests();
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Lấy chi tiết yêu cầu điều phối", 
            description = "Lấy thông tin chi tiết của một yêu cầu điều phối theo ID")
    public ResponseEntity<BaseResponse<DispatchRequestDto>> getRequestById(@PathVariable Integer id) {
        DispatchRequestDto result = requestService.getRequestById(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
