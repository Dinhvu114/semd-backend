package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.dto.response.TrackingResponse;
import com.semd.backend.service.AmbulanceJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.semd.backend.dto.response.RouteGeometryResponse;

@RestController
@RequestMapping("/api/v1/ambulance-simulations")
@Tag(name = "Ambulance Simulation", description = "Mô phỏng hành trình xe cấp cứu")
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
    public ResponseEntity<SimulationResponse> create(
            @Valid @RequestBody CreateSimulationRequest req) {
        return ResponseEntity.status(201).body(journeyService.createSimulation(req));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Bắt đầu / tiếp tục mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<SimulationResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.startSimulation(id));
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Dừng mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<SimulationResponse> stop(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.stopSimulation(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "Lấy trạng thái phiên mô phỏng",
            description = "Quyền: DISPATCHER, ADMIN"
    )
    public ResponseEntity<SimulationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.getSimulation(id));
    }

    // ── THEO DÕI HÀNH TRÌNH — REPORTER / DRIVER / DISPATCHER ──
    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Theo dõi vị trí xe theo simulation ID",
            description = "Quyền: REPORTER, DRIVER, DISPATCHER, ADMIN. " +
                    "Trả về vị trí hiện tại, phase, progress và ETA của xe cấp cứu"
    )
    public ResponseEntity<TrackingResponse> tracking(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.getTracking(id));
    }

    @GetMapping("/by-mission/{missionId}/tracking")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Theo dõi vị trí xe theo mission ID",
            description = "Quyền: REPORTER, DRIVER, DISPATCHER, ADMIN. " +
                    "Dành cho Reporter/Driver tra cứu bằng missionId " +
                    "mà không cần biết simulationId"
    )
    public ResponseEntity<TrackingResponse> trackingByMission(
            @PathVariable Integer missionId) {
        return ResponseEntity.ok(journeyService.getTrackingByMission(missionId));
    }

    // Thêm vào cuối class, sau trackingByMission:

    @GetMapping("/{id}/route")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy geometry đường đi của simulation",
            description = "Quyền: REPORTER, DRIVER, DISPATCHER, ADMIN. " +
                    "FE gọi 1 lần để vẽ polyline, không cần gọi lại mỗi tick"
    )
    public ResponseEntity<RouteGeometryResponse> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.getRoute(id));
    }
}