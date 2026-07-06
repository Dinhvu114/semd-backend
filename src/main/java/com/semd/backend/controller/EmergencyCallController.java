package com.semd.backend.controller;

import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.service.EmergencyCallService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.dto.common.BaseResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import com.semd.backend.dto.emergencyCall.*;


@RestController
@RequestMapping("/api/v1/calls")
@Tag(name = "Emergency Call", description = "API yêu cầu cấp cứu")
@CrossOrigin(origins = "*")
public class EmergencyCallController {

    private final EmergencyCallService callService;

    public EmergencyCallController(EmergencyCallService callService) {
        this.callService = callService;
    }

    @Operation(summary = "Gọi cấp cứu", description = "Tải lên ghi âm cuộc gọi cấp cứu kèm định vị")
    @PostMapping("/voice")
    public ResponseEntity<BaseResponse<Void>> createVoiceCall(
            @RequestBody VoiceCallRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Double latitude = (request.location() != null) ? request.location().latitude() : null;
        Double longitude = (request.location() != null) ? request.location().longitude() : null;

        callService.createEmergencyVoiceCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                latitude,
                longitude,
                request.audioObjectKey()
        );
        BaseResponse<Void> response = new BaseResponse<>(202, true, "Thành công", null, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Gửi định vị", description = "Tải lên định vị")
    @PostMapping("/sos")
    public ResponseEntity<BaseResponse<Void>> createSosCall(
            @RequestBody SosRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        callService.createEmergencySosCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                request.latitude(),
                request.longitude()
        );
        BaseResponse<Void> response = new BaseResponse<>(202, true, "Thành công", null, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> receiveAICallback(@RequestBody AICallbackRequest request) {
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
    public ResponseEntity<BaseResponse<List<EmergencyCall>>> getMyCalls(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<EmergencyCall> calls = callService.getMyCalls(principal.getPhoneNumber());
        return ResponseEntity.ok(BaseResponse.success(calls));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy chi tiết cuộc gọi", description = "Lấy thông tin chi tiết một cuộc gọi khẩn cấp theo ID (yêu cầu sở hữu hoặc quyền điều phối/admin)")
    public ResponseEntity<BaseResponse<EmergencyCall>> getCallDetails(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EmergencyCall call = callService.getCallDetails(id)
                .orElseThrow(() -> new com.semd.backend.exception.ResourceNotFoundException("Không tìm thấy cuộc gọi ID: " + id));

        // Phân quyền: Chỉ cho phép chính chủ xem cuộc gọi của họ, trừ khi là ADMIN hoặc DISPATCHER
        boolean isAdminOrDispatcher = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_DISPATCHER"));
        if (!isAdminOrDispatcher && !call.getReporterPhone().equals(principal.getPhoneNumber())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.fail("Bạn không có quyền xem thông tin chi tiết cuộc gọi này", 403));
        }

        return ResponseEntity.ok(BaseResponse.success(call));
    }
}
