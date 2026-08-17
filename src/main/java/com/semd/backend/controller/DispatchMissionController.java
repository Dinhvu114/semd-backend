package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.ActiveMissionResponse;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.DispatchMissionService;
import com.semd.backend.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(name = "Dispatch Mission", description = "Quản lý lệnh điều xe")
public class DispatchMissionController {

    private final DispatchMissionService missionService;
    private final IdempotencyService idempotencyService;

    public DispatchMissionController(
            DispatchMissionService missionService,
            IdempotencyService idempotencyService) {
        this.missionService = missionService;
        this.idempotencyService = idempotencyService;
    }

    // ── TẠO MISSION ───────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Tạo lệnh điều xe mới",
            description = "DISPATCHER/ADMIN. Request phải ở trạng thái RECOMMENDING")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDispatchMissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }

        DispatchMissionResponse result = missionService.create(request);
        BaseResponse<DispatchMissionResponse> body = BaseResponse.success(
                HttpStatus.CREATED.value(), "Tạo lệnh điều xe thành công", result);
        ResponseEntity<BaseResponse<DispatchMissionResponse>> response =
                ResponseEntity.status(HttpStatus.CREATED).body(body);

        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, response);
        }
        return response;
    }

    // ── XEM DANH SÁCH + CHI TIẾT ──────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Danh sách tất cả nhiệm vụ")
    public ResponseEntity<BaseResponse<List<DispatchMissionResponse>>> getAll() {
        return ResponseEntity.ok(BaseResponse.success(missionService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Chi tiết một nhiệm vụ")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(missionService.getById(id)));
    }

    // ── DRIVER: MISSION CỦA TÔI ───────────────────────────
    @GetMapping("/me/active")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Danh sách mission đang active của driver",
            description = "DRIVER. Trả về mission chưa hoàn thành được giao cho tài xế đang đăng nhập")
    public ResponseEntity<BaseResponse<List<ActiveMissionResponse>>> getMyActiveMissions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                missionService.getMyActiveMissions(principal.getId())));
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lịch sử mission của driver",
            description = "DRIVER. Trả về các mission đã COMPLETED/REJECTED/CANCELLED")
    public ResponseEntity<BaseResponse<List<ActiveMissionResponse>>> getMyMissionHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                missionService.getMyMissionHistory(principal.getId())));
    }

    // ── DRIVER: NHẬN / TỪ CHỐI ────────────────────────────
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver xác nhận nhận nhiệm vụ",
            description = "DRIVER. Mission phải ở trạng thái DISPATCHED")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> accept(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Nhận nhiệm vụ thành công",
                missionService.accept(id, principal.getId())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver từ chối nhiệm vụ",
            description = "DRIVER. Bắt buộc kèm lý do. Mission phải ở trạng thái DISPATCHED")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> reject(
            @PathVariable Integer id,
            @Valid @RequestBody RejectMissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Từ chối nhiệm vụ thành công",
                missionService.reject(id, request, principal.getId())));
    }

    // ── DRIVER: CẬP NHẬT HÀNH TRÌNH ──────────────────────
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver bắt đầu di chuyển",
            description = "DRIVER. Mission phải ACCEPTED. Tự động start simulation nếu có.")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> start(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Bắt đầu di chuyển đến hiện trường",
                missionService.start(id, principal.getId())));
    }

    @PostMapping("/{id}/arrive-scene")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver báo đã đến hiện trường",
            description = "DRIVER. Mission phải ở trạng thái EN_ROUTE")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveScene(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Đã cập nhật trạng thái đến hiện trường",
                missionService.arriveScene(id, principal.getId())));
    }

    @PostMapping("/{id}/start-transport")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver bắt đầu chở bệnh nhân",
            description = "DRIVER. Mission phải ở trạng thái ARRIVED_SCENE")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> startTransport(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Bắt đầu vận chuyển bệnh nhân",
                missionService.startTransport(id, principal.getId())));
    }

    @PostMapping("/{id}/arrive-hospital")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver báo đã đến bệnh viện",
            description = "DRIVER. Mission phải ở trạng thái TRANSPORTING")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveHospital(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Đã cập nhật trạng thái đến bệnh viện",
                missionService.arriveHospital(id, principal.getId())));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Hoàn thành nhiệm vụ — đóng ca, giải phóng xe",
            description = "DRIVER. Mission phải ở trạng thái ARRIVED_HOSPITAL")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> complete(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "Hoàn thành nhiệm vụ thành công",
                missionService.complete(id, principal.getId())));
    }

    // ── DISPATCHER: REDISPATCH ─────────────────────────────
    @PostMapping("/redispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Điều xe khác khi driver từ chối",
            description = "DISPATCHER, ADMIN. Request phải ở trạng thái DISPATCHED")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> redispatch(
            @RequestParam Integer requestId,
            @RequestParam Integer newResourceId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Điều phối lại thành công",
                missionService.redispatch(requestId, newResourceId)));
    }
}