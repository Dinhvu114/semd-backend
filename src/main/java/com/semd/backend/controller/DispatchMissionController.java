package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateDispatchMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.service.DispatchMissionService;
import com.semd.backend.service.IdempotencyService;
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
    private final IdempotencyService idempotencyService;

    public DispatchMissionController(DispatchMissionService missionService, IdempotencyService idempotencyService) {
        this.missionService = missionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "Tạo lệnh điều xe mới",
            description = "Tạo mission + tự động gửi thông báo WebSocket cho Dispatcher")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDispatchMissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null && idempotencyService.isDuplicate(idempotencyKey)) {
            return idempotencyService.getResponse(idempotencyKey);
        }
        DispatchMissionResponse result = missionService.create(request);
        ResponseEntity<DispatchMissionResponse> resEntity = ResponseEntity.status(201).body(result);
        if (idempotencyKey != null) {
            idempotencyService.save(idempotencyKey, resEntity);
        }
        return resEntity;
    }
}