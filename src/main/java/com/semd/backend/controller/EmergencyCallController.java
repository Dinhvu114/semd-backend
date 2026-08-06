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
@Tag(name = "Emergency Call", description = "API yêu cầu cấp cứu")
@CrossOrigin(origins = "*")
public class EmergencyCallController {

    private final EmergencyCallService callService;
    private final IdempotencyService idempotencyService;

    @org.springframework.beans.factory.annotation.Value("${app.callback-key}")
    private String serverCallbackKey;

    public EmergencyCallController(EmergencyCallService callService, IdempotencyService idempotencyService) {
        this.callService = callService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Gọi cấp cứu", description = "Tải lên ghi âm cuộc gọi cấp cứu kèm định vị")
    @PostMapping("/voice")
    public ResponseEntity<?> createVoiceCall(
            @Valid @RequestBody VoiceCallRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Double latitude = (request.location() != null) ? request.location().latitude() : null;
        Double longitude = (request.location() != null) ? request.location().longitude() : null;

        EmergencyCallResponse createdCall = callService.createEmergencyVoiceCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                latitude,
                longitude,
                request.audioObjectKey(),
                request.description()
        );
        BaseResponse<EmergencyCallResponse> response = BaseResponse.success(202, "Đã tiếp nhận cuộc gọi khẩn cấp", createdCall);
        ResponseEntity<BaseResponse<EmergencyCallResponse>> resEntity = ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, resEntity);
        }
        return resEntity;
    }

    @Operation(summary = "Gửi định vị", description = "Tải lên định vị")
    @PostMapping("/sos")
    public ResponseEntity<?> createSosCall(
            @Valid @RequestBody SosRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EmergencyCallResponse createdCall = callService.createEmergencySosCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                request.latitude(),
                request.longitude(),
                request.description()
        );
        BaseResponse<EmergencyCallResponse> response = BaseResponse.success(202, "Đã tiếp nhận yêu cầu SOS", createdCall);
        ResponseEntity<BaseResponse<EmergencyCallResponse>> resEntity = ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, resEntity);
        }
        return resEntity;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> receiveAICallback(
            @RequestBody AICallbackRequest request,
            @RequestHeader(value = "X-Callback-Key", required = false) String clientCallbackKey
    ) {
        if (clientCallbackKey == null || !clientCallbackKey.equals(serverCallbackKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        callService.handleAICallback(
                request.call_id(),
                request.transcript(),
                request.urgency(),
                request.confidence(),
                request.symptoms()
        );
        return ResponseEntity.ok().build();
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
        EmergencyCallResponse call = callService.getCallDetails(id)
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException("Không tìm thấy cuộc gọi ID: " + id));

        // Phân quyền: Chỉ cho phép chính chủ xem cuộc gọi của họ, trừ khi là ADMIN hoặc DISPATCHER
        boolean isAdminOrDispatcher = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (!isAdminOrDispatcher && !call.reporterPhone().equals(principal.getPhoneNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail("Bạn không có quyền xem thông tin chi tiết cuộc gọi này", 403));
        }

        return ResponseEntity.ok(BaseResponse.success(call));
    }
}
