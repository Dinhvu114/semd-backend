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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.semd.backend.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(
        name = "DISPATCHER - Mission",
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
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(
            summary = "Tạo lệnh điều xe mới",
            description = "DISPATCHER. Request phải ở trạng thái RECOMMENDING"
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

    @GetMapping()
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<BaseResponse<List<DispatchMissionResponse>>> getAll() {
        List<DispatchMissionResponse> result =
                missionService.getAll();

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DISPATCHER')")
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


    // ── DISPATCHER: REDISPATCH ─────────────────────────────
    @PostMapping("/redispatch")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(
            summary = "Điều xe khác khi driver từ chối",
            description = "DISPATCHER. Request phải ở trạng thái DISPATCHED"
    )
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> redispatch(
            @RequestParam Integer requestId,
            @RequestParam Integer newResourceId) {
        DispatchMissionResponse result =
            missionService.redispatch(requestId, newResourceId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        201,
                        "Điều phối lại thành công",
                        result
                ));
        }
}
