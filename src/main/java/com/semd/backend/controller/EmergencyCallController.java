package com.semd.backend.controller;

import com.semd.backend.entity.EmergencyCall;
import com.semd.backend.service.EmergencyCallService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.semd.backend.security.UserPrincipal;


@RestController
@RequestMapping("/api/v1/calls")
@Tag(name = "Emergency Call", description = "API yêu cầu cấp cứu")
@CrossOrigin(origins = "*") // Hỗ trợ CORS cho FE gọi thử nghiệm
public class EmergencyCallController {

    private final EmergencyCallService callService;

    public EmergencyCallController(EmergencyCallService callService) {
        this.callService = callService;
    }

    /**
     * API SOS: Tải lên ghi âm cuộc gọi cấp cứu kèm định vị.
     * Sử dụng JWT để tự động trích xuất thông tin người báo cáo (không cho phép truyền thủ công).
     * Endpoint: POST /api/v1/calls/voice
     */
    @PostMapping("/voice")
    public ResponseEntity<EmergencyCall> createVoiceCall(
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
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(call);
    }

    /**
     * API SOS: Báo khẩn cấp nhanh (panic button) kèm định vị, không kèm file ghi âm.
     * Sử dụng JWT để tự động trích xuất thông tin người báo cáo (không cho phép truyền thủ công).
     * Endpoint: POST /api/v1/calls/sos
     */
    @PostMapping("/sos")
    public ResponseEntity<EmergencyCall> createSosCall(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(call);
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

    /**
     * Hợp đồng kết nối (DTO) nhận kết quả từ FastAPI
     */
    public record AICallbackRequest(
        Integer call_id,
        String transcript,
        String urgency,
        Double confidence,
        java.util.List<String> symptoms
    ) {}


    /**
     * Hợp đồng nhận yêu cầu SOS khẩn cấp (chỉ cần tọa độ vì tên và SĐT lấy từ JWT)
     */
    public record SosRequest(
        Double latitude,
        Double longitude
    ) {}

    /**
     * Hợp đồng nhận yêu cầu SOS kèm file ghi âm (chỉ cần object key và tọa độ vì thông tin cá nhân lấy từ JWT)
     */
    public record VoiceCallRequest(
        String audioObjectKey,
        LocationDto location
    ) {}

    public record LocationDto(
        Double latitude,
        Double longitude
    ) {}
}
