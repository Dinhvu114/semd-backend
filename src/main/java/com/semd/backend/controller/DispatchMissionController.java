package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.service.DispatchMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(name = "Dispatch Mission", description = "Quản lý lệnh điều xe")
public class DispatchMissionController {

    private final DispatchMissionService missionService;

    public DispatchMissionController(DispatchMissionService missionService) {
        this.missionService = missionService;
    }

    @PostMapping
    @Operation(summary = "Tạo lệnh điều xe mới",
            description = "Tạo mission + tự động gửi thông báo WebSocket cho Dispatcher")
    public ResponseEntity<DispatchMissionResponse> create(
            @Valid @RequestBody CreateDispatchMissionRequest request) {
        return ResponseEntity.status(201).body(missionService.create(request));
    }
}