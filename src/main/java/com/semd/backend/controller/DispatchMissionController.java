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
        public ResponseEntity<?> create(
                @Valid @RequestBody CreateDispatchMissionRequest request,
                @RequestHeader(
                        value = "Idempotency-Key",
                        required = false
                ) String idempotencyKey,
                @AuthenticationPrincipal UserPrincipal principal
        ) {
        return idempotencyService.execute(
                "dispatch:create",
                principal.getId(),
                idempotencyKey,
                () -> {
                        DispatchMissionResponse result =
                                missionService.create(request);

                        return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                        BaseResponse.success(
                                                201,
                                                "Tạo lệnh điều xe thành công",
                                                result
                                        )
                                );
                }
        );
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
        public ResponseEntity<?> redispatch(
                @RequestParam Integer requestId,
                @RequestParam Integer newResourceId,
                @RequestHeader(
                        value = "Idempotency-Key",
                        required = false
                ) String idempotencyKey,
                @AuthenticationPrincipal UserPrincipal principal
        ) {
        return idempotencyService.execute(
                "dispatch:redispatch",
                principal.getId(),
                idempotencyKey,
                () -> {
                        DispatchMissionResponse result =
                                missionService.redispatch(
                                        requestId,
                                        newResourceId
                                );

                        return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                        BaseResponse.success(
                                                201,
                                                "Điều phối lại thành công",
                                                result
                                        )
                                );
                }
        );
        }
}
