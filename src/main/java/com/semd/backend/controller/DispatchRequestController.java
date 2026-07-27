package com.semd.backend.controller;

import com.semd.backend.dto.DispatchRequestDto;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.ConfirmDispatchRequest;
import com.semd.backend.dto.request.RejectDispatchRequest;
import com.semd.backend.dto.request.SeverityUpdateRequest;
import com.semd.backend.dto.request.VerifyDispatchRequest;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.dto.response.*;
import com.semd.backend.service.DispatchRequestService;
import com.semd.backend.entity.DispatchRequestStatus;
import com.semd.backend.dto.common.Metadata;
import com.semd.backend.dto.common.PageRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dispatch-requests")
@Tag(name = "Dispatch Requests", description = "Quản lý yêu cầu điều phối cấp cứu")
public class DispatchRequestController {

    private final DispatchRequestService requestService;

    public DispatchRequestController(DispatchRequestService requestService) {
        this.requestService = requestService;
    }

    // ──────────────────────────────────────────────
    // 11. GET /statistics  (đặt trước /{id} để tránh conflict)
    // ──────────────────────────────────────────────
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Thống kê dashboard",
               description = "Trả về số lượng yêu cầu theo từng trạng thái và số ca hoàn thành hôm nay")
    public ResponseEntity<BaseResponse<StatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(BaseResponse.success(requestService.getStatistics()));
    }

    // ──────────────────────────────────────────────
    // 1. GET /dispatch-requests
    // ──────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Danh sách hàng đợi điều phối",
               description = "Lọc và phân trang hàng đợi điều phối, sắp xếp theo thời gian tạo giảm dần")
    public ResponseEntity<BaseResponse<List<DispatchRequestDto>>> getAllRequests(
            @Parameter(description = "Lọc theo trạng thái")
            @RequestParam(required = false) DispatchRequestStatus status,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) Integer serviceTypeId,
            @RequestParam(required = false) Integer edgeNodeId,
            @ParameterObject @ModelAttribute PageRequestDto pagination) {
        Page<DispatchRequestDto> result = requestService.search(
                status, urgencyLevel, serviceTypeId, edgeNodeId,
                pagination.toPageable(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return ResponseEntity.ok(BaseResponse.success(result.getContent(), Metadata.from(result)));
    }

    // ──────────────────────────────────────────────
    // 2. GET /dispatch-requests/{id}
    // ──────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Chi tiết yêu cầu điều phối",
               description = "Trả về đầy đủ thông tin: cuộc gọi, kết quả AI, số lần điều xe, trạng thái")
    public ResponseEntity<BaseResponse<DispatchRequestDetailDto>> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(requestService.getDetail(id)));
    }

    // ──────────────────────────────────────────────
    // 3. POST /dispatch-requests/{id}/analyze
    // ──────────────────────────────────────────────
    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Phân tích AI đồng bộ",
               description = "Dispatcher yêu cầu AI phân tích ghi âm ngay lập tức, kết quả trả về trong cùng request")
    public ResponseEntity<BaseResponse<Map<String, Object>>> analyze(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(requestService.analyze(id)));
    }

    // ──────────────────────────────────────────────
    // 4. POST /dispatch-requests/{id}/confirm
    // ──────────────────────────────────────────────
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Xác nhận ca cấp cứu",
               description = "Dispatcher xác nhận đây là ca thật, chuyển trạng thái sang CONFIRMED")
    public ResponseEntity<BaseResponse<Map<String, String>>> confirm(
            @PathVariable Integer id,
            @RequestBody ConfirmDispatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                requestService.confirm(id, request, principal.getId())));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Xác minh yêu cầu điều phối",
            description = "Chuyển trạng thái PENDING sang CONFIRMED sau khi kiểm tra EmergencyCall")
    public ResponseEntity<BaseResponse<DispatchRequestDetailDto>> verify(
            @PathVariable Integer id,
            @RequestBody VerifyDispatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                requestService.verify(id, request, principal.getId())));
    }

    // ──────────────────────────────────────────────
    // 5. POST /dispatch-requests/{id}/reject
    // ──────────────────────────────────────────────
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Từ chối báo động giả",
               description = "Dispatcher từ chối yêu cầu, chuyển trạng thái sang REJECTED")
    public ResponseEntity<BaseResponse<Map<String, String>>> reject(
            @PathVariable Integer id,
            @RequestBody RejectDispatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                requestService.reject(id, request, principal.getId())));
    }

    // ──────────────────────────────────────────────
    // 6. PATCH /dispatch-requests/{id}/severity
    // ──────────────────────────────────────────────
    @PatchMapping("/{id}/severity")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Cập nhật mức độ nghiêm trọng",
               description = "Dispatcher override mức độ phân loại của AI (ví dụ: RED, ORANGE, YELLOW, GREEN)")
    public ResponseEntity<BaseResponse<Map<String, String>>> updateSeverity(
            @PathVariable Integer id,
            @RequestBody SeverityUpdateRequest request) {
        return ResponseEntity.ok(BaseResponse.success(requestService.updateSeverity(id, request)));
    }

    // ──────────────────────────────────────────────
    // 7. GET /dispatch-requests/{id}/recommendations
    // ──────────────────────────────────────────────
    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Gợi ý top 3 xe cứu thương",
               description = "Lọc xe hợp lệ và xếp hạng theo ETA, khoảng cách, năng lực, độ mới vị trí và rủi ro")
    public ResponseEntity<BaseResponse<List<RecommendationItemDto>>> recommend(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(requestService.recommend(id)));
    }

    // ──────────────────────────────────────────────
    // 8. POST /dispatch-requests/{id}/redispatch
    // ──────────────────────────────────────────────
    @PostMapping("/{id}/redispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Điều phối lại",
               description = "Hủy mission đang chạy, giải phóng xe cũ, tạo mission mới với xe khác")
    public ResponseEntity<BaseResponse<Map<String, Object>>> redispatch(
            @PathVariable Integer id,
            @RequestParam Integer newResourceId) {
        return ResponseEntity.ok(BaseResponse.success(requestService.redispatch(id, newResourceId)));
    }

    // ──────────────────────────────────────────────
    // 9. POST /dispatch-requests/{id}/cancel
    // ──────────────────────────────────────────────
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Hủy yêu cầu điều phối",
               description = "Hủy yêu cầu, tự động giải phóng xe đang gắn (nếu có) về trạng thái AVAILABLE")
    public ResponseEntity<BaseResponse<Map<String, String>>> cancel(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(requestService.cancel(id)));
    }

    // ──────────────────────────────────────────────
    // 10. GET /dispatch-requests/{id}/timeline
    // ──────────────────────────────────────────────
    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Lịch sử xử lý yêu cầu",
               description = "Trả về chuỗi sự kiện theo thứ tự thời gian: CALL_RECEIVED → AI_ANALYZED → CONFIRMED → MISSION_*")
    public ResponseEntity<BaseResponse<List<TimelineEventDto>>> getTimeline(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(requestService.getTimeline(id)));
    }

}
