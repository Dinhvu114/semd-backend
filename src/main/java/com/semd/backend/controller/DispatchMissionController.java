package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.service.DispatchMissionService;
import com.semd.backend.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(
        name = "Dispatch Mission",
        description = "Quản lý lệnh điều xe"
)
public class DispatchMissionController {

    private final DispatchMissionService missionService;
    private final IdempotencyService idempotencyService;

    public DispatchMissionController(
            DispatchMissionService missionService,
            IdempotencyService idempotencyService
    ) {
        this.missionService = missionService;
        this.idempotencyService = idempotencyService;
    }

    // ── TẠO MISSION ───────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Tạo lệnh điều xe mới",
            description = "DISPATCHER/ADMIN. Request phải ở trạng thái CONFIRMED"
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDispatchMissionRequest request,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyKey
    ) {
        if (idempotencyKey != null
                && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }

        DispatchMissionResponse result =
                missionService.create(request);

        BaseResponse<DispatchMissionResponse> body =
                BaseResponse.success(
                        HttpStatus.CREATED.value(),
                        "Tạo lệnh điều xe thành công",
                        result
                );

        ResponseEntity<BaseResponse<DispatchMissionResponse>> response =
                ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(body);

        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, response);
        }

        return response;
    }

    // ── XEM DANH SÁCH + CHI TIẾT ──────────────────────────

    @GetMapping
    @Operation(
            summary = "Danh sách tất cả nhiệm vụ",
            description = "DISPATCHER/ADMIN/DRIVER"
    )
    public ResponseEntity<BaseResponse<List<DispatchMissionResponse>>> getAll() {
        List<DispatchMissionResponse> result =
                missionService.getAll();

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một nhiệm vụ")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> getById(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.getById(id);

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    // ── DRIVER: NHẬN / TỪ CHỐI ────────────────────────────

    @PostMapping("/{id}/accept")
    @Operation(
            summary = "Driver xác nhận nhận nhiệm vụ",
            description = "DRIVER. Mission phải ở trạng thái DISPATCHED"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> accept(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.accept(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Nhận nhiệm vụ thành công",
                        result
                )
        );
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Driver từ chối nhiệm vụ",
            description = """
                    DRIVER. Bắt buộc kèm lý do.
                    Mission phải ở trạng thái DISPATCHED
                    """
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> reject(
            @PathVariable Integer id,
            @Valid @RequestBody RejectMissionRequest request
    ) {
        DispatchMissionResponse result =
                missionService.reject(id, request);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Từ chối nhiệm vụ thành công",
                        result
                )
        );
    }

    // ── DRIVER: CẬP NHẬT HÀNH TRÌNH ──────────────────────

    @PostMapping("/{id}/start")
    @Operation(
            summary = "Driver bắt đầu di chuyển đến hiện trường",
            description = "DRIVER. Mission phải ở trạng thái ACCEPTED"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> start(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.start(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu di chuyển đến hiện trường",
                        result
                )
        );
    }

    @PostMapping("/{id}/arrive-scene")
    @Operation(
            summary = "Driver báo đã đến hiện trường",
            description = "DRIVER. Mission phải ở trạng thái EN_ROUTE"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveScene(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.arriveScene(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Đã cập nhật trạng thái đến hiện trường",
                        result
                )
        );
    }

    @PostMapping("/{id}/start-transport")
    @Operation(
            summary = "Driver bắt đầu chở bệnh nhân",
            description = "DRIVER. Mission phải ở trạng thái ARRIVED_SCENE"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> startTransport(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.startTransport(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu vận chuyển bệnh nhân",
                        result
                )
        );
    }

    @PostMapping("/{id}/arrive-hospital")
    @Operation(
            summary = "Driver báo đã đến bệnh viện",
            description = "DRIVER. Mission phải ở trạng thái TRANSPORTING"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveHospital(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.arriveHospital(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Đã cập nhật trạng thái đến bệnh viện",
                        result
                )
        );
    }

    @PostMapping("/{id}/complete")
    @Operation(
            summary = "Hoàn thành nhiệm vụ — đóng ca, giải phóng xe",
            description = """
                    DRIVER.
                    Mission phải ở trạng thái ARRIVED_HOSPITAL
                    """
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> complete(
            @PathVariable Integer id
    ) {
        DispatchMissionResponse result =
                missionService.complete(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Hoàn thành nhiệm vụ thành công",
                        result
                )
        );
    }

    // ── DISPATCHER: REDISPATCH ─────────────────────────────
    @PostMapping("/redispatch")
    @Operation(
            summary = "Điều xe khác khi driver từ chối",
            description = "DISPATCHER, ADMIN. Request phải ở trạng thái DISPATCHED"
    )
    public ResponseEntity<DispatchMissionResponse> redispatch(
            @RequestParam Integer requestId,
            @RequestParam Integer newResourceId) {
        return ResponseEntity.status(201).body(
                missionService.redispatch(requestId, newResourceId));
    }

    // ── ENDPOINT CŨ ───────────────────────────────────────

    @Deprecated
    @PatchMapping("/{missionId}/status")
    @Operation(
            summary = "[Deprecated] Cập nhật trạng thái mission",
            description = """
                    Endpoint cũ, chỉ giữ để tương thích.
                    Hãy sử dụng các endpoint chuyển trạng thái riêng.
                    """
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> updateStatus(
            @PathVariable Integer missionId,
            @RequestParam String status
    ) {
        DispatchMissionResponse result =
                missionService.updateStatus(missionId, status);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Cập nhật trạng thái nhiệm vụ thành công",
                        result
                )
        );
    }
}