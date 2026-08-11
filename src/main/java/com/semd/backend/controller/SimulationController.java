package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.dto.response.TrackingResponse;
import com.semd.backend.service.AmbulanceJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ambulance-simulations")
@Tag(
        name = "Ambulance Simulation",
        description = "Mô phỏng hành trình xe cấp cứu"
)
public class SimulationController {

    private final AmbulanceJourneyService journeyService;

    public SimulationController(AmbulanceJourneyService journeyService) {
        this.journeyService = journeyService;
    }

    // ── DISPATCHER / ADMIN ─────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Tạo phiên mô phỏng mới",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> create(
            @Valid @RequestBody CreateSimulationRequest req
    ) {
        SimulationResponse result =
                journeyService.createSimulation(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        BaseResponse.success(
                                HttpStatus.CREATED.value(),
                                "Tạo phiên mô phỏng thành công",
                                result
                        )
                );
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Bắt đầu / tiếp tục mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> start(
            @PathVariable Long id
    ) {
        SimulationResponse result =
                journeyService.startSimulation(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu mô phỏng thành công",
                        result
                )
        );
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Dừng mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> stop(
            @PathVariable Long id
    ) {
        SimulationResponse result =
                journeyService.stopSimulation(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Dừng mô phỏng thành công",
                        result
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Lấy trạng thái phiên mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> get(
            @PathVariable Long id
    ) {
        SimulationResponse result =
                journeyService.getSimulation(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        result
                )
        );
    }

    // ── THEO DÕI HÀNH TRÌNH — REPORTER / DRIVER / DISPATCHER ──

    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Theo dõi vị trí xe theo simulation ID",
            description = "Quyền: REPORTER, DRIVER, DISPATCHER, ADMIN. " +
                    "Trả về vị trí hiện tại, phase, progress và ETA của xe cấp cứu"
    )
    public ResponseEntity<BaseResponse<TrackingResponse>> tracking(
            @PathVariable Long id
    ) {
        TrackingResponse result =
                journeyService.getTracking(id);

        return ResponseEntity.ok(
                BaseResponse.success(
                        result
                )
        );
    }

    @GetMapping("/by-mission/{missionId}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Theo dõi vị trí xe theo mission ID",
            description = "Quyền: REPORTER, DRIVER, DISPATCHER, ADMIN. " +
                    "Dành cho Reporter/Driver tra cứu bằng missionId " +
                    "mà không cần biết simulationId"
    )
    public ResponseEntity<BaseResponse<TrackingResponse>> trackingByMission(
            @PathVariable Integer missionId
    ) {
        TrackingResponse result =
                journeyService.getTrackingByMission(missionId);

        return ResponseEntity.ok(
                BaseResponse.success(
                        result
                )
        );
    }
}