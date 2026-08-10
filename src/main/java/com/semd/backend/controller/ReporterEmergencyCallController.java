package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.emergencyCall.EmergencyCallResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.EmergencyCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emergency-calls")
@Tag(name = "Reporter Emergency Call", description = "API yêu cầu cấp cứu của người báo tin")
public class ReporterEmergencyCallController {
    private final EmergencyCallService callService;

    public ReporterEmergencyCallController(EmergencyCallService callService) {
        this.callService = callService;
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
}
