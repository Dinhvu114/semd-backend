package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.emergencyCall.AICallbackRequest;
import com.semd.backend.dto.emergencyCall.EmergencyCallResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.EmergencyCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emergency-calls")
@Tag(name = "Emergency Callback", description = "API callback")
public class SystemEmergencyCallController {
    private final EmergencyCallService callService;

    @org.springframework.beans.factory.annotation.Value("${app.callback-key}")
    private String serverCallbackKey;
    public SystemEmergencyCallController(EmergencyCallService callService) {
        this.callService = callService;
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
}
