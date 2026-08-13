package com.semd.backend.controller;

import com.semd.backend.service.EmergencyCallService;
import com.semd.backend.service.IdempotencyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.dto.common.BaseResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.util.List;
import com.semd.backend.dto.emergencyCall.*;


@RestController
@RequestMapping("/api/v1/calls")
@Tag(name = "Reporter Emergency Call", description = "API yêu cầu cấp cứu")

@CrossOrigin(origins = "*")
public class ReporterEmergencyCallController {

    private final EmergencyCallService callService;
    private final IdempotencyService idempotencyService;

    public ReporterEmergencyCallController(EmergencyCallService callService, IdempotencyService idempotencyService) {
        this.callService = callService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Gọi cấp cứu", description = "Tải lên ghi âm cuộc gọi cấp cứu kèm định vị")
    @PostMapping("/voice")
    public ResponseEntity<?> createVoiceCall(
        @Valid @RequestBody VoiceCallRequest request,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestHeader(
            value = "Idempotency-Key",
            required = false
        ) String idempotencyKey
    ) {
    if (principal == null) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .build();
    }

    return idempotencyService.execute(
        "call:voice",
        principal.getId(),
        idempotencyKey,
        () -> {
            Double latitude =
                request.location() != null
                    ? request.location().latitude()
                    : null;

            Double longitude =
                request.location() != null
                    ? request.location().longitude()
                    : null;

            EmergencyCallResponse createdCall =
                callService.createEmergencyVoiceCall(
                    principal.getPhoneNumber(),
                    principal.getFullName(),
                    latitude,
                    longitude,
                    request.audioObjectKey(),
                    request.description()
                );

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                    BaseResponse.success(
                        202,
                        "Đã tiếp nhận cuộc gọi khẩn cấp",
                        createdCall
                    )
                );
            }
        );
    }

    @Operation(summary = "Gửi định vị", description = "Tải lên định vị")
    @PostMapping("/sos")
    public ResponseEntity<?> createSosCall(
        @Valid @RequestBody SosRequest request,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestHeader(
            value = "Idempotency-Key",
            required = false
        ) String idempotencyKey
    ) {
    if (principal == null) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .build();
    }

    return idempotencyService.execute(
        "call:sos",
        principal.getId(),
        idempotencyKey,
        () -> {
        EmergencyCallResponse createdCall =
            callService.createEmergencySosCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                request.latitude(),
                request.longitude(),
                request.description()
            );

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                BaseResponse.success(
                    202,
                    "Đã tiếp nhận yêu cầu SOS",
                    createdCall
                    )
                );
            }
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lịch sử yêu cầu cấp cứu đã gửi")
    public ResponseEntity<BaseResponse<List<EmergencyCallResponse>>> getMyEmergencyCalls(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                callService.getMyCalls(principal.getPhoneNumber())));
    }
    
    @GetMapping("/my-calls")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách cuộc gọi của tôi", description = "Lấy toàn bộ danh sách cuộc gọi khẩn cấp do chính tài khoản này tạo ra")
    public ResponseEntity<BaseResponse<List<EmergencyCallResponse>>> getMyCalls(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<EmergencyCallResponse> calls = callService.getMyCalls(principal.getPhoneNumber());
        return ResponseEntity.ok(BaseResponse.success(calls));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết cuộc gọi", description = "Lấy thông tin chi tiết một cuộc gọi khẩn cấp theo ID (yêu cầu sở hữu hoặc quyền điều phối/admin)")
    public ResponseEntity<BaseResponse<EmergencyCallResponse>> getCallDetails(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EmergencyCallResponse call = callService.getOwnedCallDetails(id, principal.getPhoneNumber());
        return ResponseEntity.ok(BaseResponse.success(call));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Trạng thái xử lý yêu cầu của tôi")
    public ResponseEntity<BaseResponse<CallStatusResponse>> getCallStatus(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                callService.getOwnedCallStatus(id, principal.getPhoneNumber())));
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Theo dõi yêu cầu và xe cấp cứu của tôi")
    public ResponseEntity<BaseResponse<CallTrackingResponse>> getCallTracking(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                callService.getOwnedCallTracking(id, principal.getPhoneNumber())));
    }
}
