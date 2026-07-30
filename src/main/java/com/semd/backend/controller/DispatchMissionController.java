package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.service.DispatchMissionService;
import com.semd.backend.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(name = "Dispatch Mission", description = "Quản lý lệnh điều xe")
public class DispatchMissionController {

    private final DispatchMissionService missionService;
    private final IdempotencyService idempotencyService;

    public DispatchMissionController(DispatchMissionService missionService,
                                     IdempotencyService idempotencyService) {
        this.missionService = missionService;
        this.idempotencyService = idempotencyService;
    }

    // ── TẠO MISSION ───────────────────────────────────────
    @PostMapping
    @Operation(summary = "Tạo lệnh điều xe mới",
            description = "DISPATCHER/ADMIN. Request phải ở trạng thái VERIFIED")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDispatchMissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }
        DispatchMissionResponse result = missionService.create(request);
        ResponseEntity<DispatchMissionResponse> resEntity = ResponseEntity.status(201).body(result);
        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, resEntity);
        }
        return resEntity;
    }

    // ── XEM DANH SÁCH + CHI TIẾT ──────────────────────────
    @GetMapping
    @Operation(summary = "Danh sách tất cả nhiệm vụ",
            description = "DISPATCHER/ADMIN/DRIVER")
    public ResponseEntity<List<DispatchMissionResponse>> getAll() {
        return ResponseEntity.ok(missionService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một nhiệm vụ")
    public ResponseEntity<DispatchMissionResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.getById(id));
    }

    // ── DRIVER: NHẬN / TỪ CHỐI ────────────────────────────
    @PostMapping("/{id}/accept")
    @Operation(summary = "Driver xác nhận nhận nhiệm vụ",
            description = "DRIVER. Mission phải ở trạng thái DISPATCHED")
    public ResponseEntity<DispatchMissionResponse> accept(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.accept(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Driver từ chối nhiệm vụ",
            description = "DRIVER. Bắt buộc kèm lý do. Mission phải ở trạng thái DISPATCHED")
    public ResponseEntity<DispatchMissionResponse> reject(
            @PathVariable Integer id,
            @Valid @RequestBody RejectMissionRequest req) {
        return ResponseEntity.ok(missionService.reject(id, req));
    }

    // ── DRIVER: CẬP NHẬT HÀNH TRÌNH ──────────────────────
    @PostMapping("/{id}/start")
    @Operation(summary = "Driver bắt đầu di chuyển đến hiện trường",
            description = "DRIVER. Mission phải ở trạng thái ACCEPTED")
    public ResponseEntity<DispatchMissionResponse> start(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.start(id));
    }

    @PostMapping("/{id}/arrive-scene")
    @Operation(summary = "Driver báo đã đến hiện trường",
            description = "DRIVER. Mission phải ở trạng thái EN_ROUTE")
    public ResponseEntity<DispatchMissionResponse> arriveScene(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.arriveScene(id));
    }

    @PostMapping("/{id}/start-transport")
    @Operation(summary = "Driver bắt đầu chở bệnh nhân",
            description = "DRIVER. Mission phải ở trạng thái ARRIVED_SCENE")
    public ResponseEntity<DispatchMissionResponse> startTransport(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.startTransport(id));
    }

    @PostMapping("/{id}/arrive-hospital")
    @Operation(summary = "Driver báo đã đến bệnh viện",
            description = "DRIVER. Mission phải ở trạng thái TRANSPORTING")
    public ResponseEntity<DispatchMissionResponse> arriveHospital(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.arriveHospital(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Hoàn thành nhiệm vụ — đóng ca, giải phóng xe",
            description = "DRIVER. Mission phải ở trạng thái ARRIVED_HOSPITAL")
    public ResponseEntity<DispatchMissionResponse> complete(@PathVariable Integer id) {
        return ResponseEntity.ok(missionService.complete(id));
    }

    // ── GIỮ LẠI endpoint cũ để không break ───────────────
    @PatchMapping("/{missionId}/status")
    @Operation(summary = "[Cũ] Cập nhật trạng thái — dùng các endpoint riêng phía trên thay thế")
    public ResponseEntity<DispatchMissionResponse> updateStatus(
            @PathVariable Integer missionId,
            @RequestParam String status) {
        return ResponseEntity.ok(missionService.updateStatus(missionId, status));
    }
}