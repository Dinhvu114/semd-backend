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
public class SimulationTrackingController {

    private final AmbulanceJourneyService journeyService;

    public SimulationTrackingController(AmbulanceJourneyService journeyService) {
        this.journeyService = journeyService;
    }

    // ── THEO DÕI HÀNH TRÌNH — REPORTER / DRIVER / ──

    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Theo dõi vị trí xe theo simulation ID",
            description = "Quyền: REPORTER, DRIVER." +
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
            description = "Quyền: REPORTER, DRIVER. " +
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