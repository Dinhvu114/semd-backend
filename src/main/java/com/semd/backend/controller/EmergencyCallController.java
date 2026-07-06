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
    public ResponseEntity<BaseResponse<EmergencyCall>> createVoiceCall(
            @RequestBody VoiceCallRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Double latitude = (request.location() != null) ? request.location().latitude() : null;
        Double longitude = (request.location() != null) ? request.location().longitude() : null;

        EmergencyCall call = callService.createEmergencyVoiceCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                latitude,
                longitude,
                request.audioObjectKey()
        );
        BaseResponse<EmergencyCall> response = new BaseResponse<>(202, true, "Thành công", call, null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Gửi định vị", description = "Tải lên định vị")
    @PostMapping("/sos")
    public ResponseEntity<BaseResponse<EmergencyCall>> createSosCall(
            @RequestBody SosRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        EmergencyCall call = callService.createEmergencySosCall(
                principal.getPhoneNumber(),
                principal.getFullName(),
                request.latitude(),
                request.longitude()
        );
        BaseResponse<EmergencyCall> response = new BaseResponse<>(200, true, "Thành công", call, null);
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
}
