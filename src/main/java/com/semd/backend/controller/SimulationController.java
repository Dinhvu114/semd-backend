package com.semd.backend.controller;

import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
import com.semd.backend.service.AmbulanceJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ambulance-simulations")
@Tag(name = "Ambulance Simulation", description = "Mô phỏng hành trình xe cấp cứu")
public class SimulationController {

    private final AmbulanceJourneyService journeyService;

    public SimulationController(AmbulanceJourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping
    @Operation(summary = "Tạo phiên mô phỏng mới")
    public ResponseEntity<SimulationResponse> create(
            @Valid @RequestBody CreateSimulationRequest req) {
        return ResponseEntity.status(201).body(journeyService.createSimulation(req));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Bắt đầu / tiếp tục mô phỏng")
    public ResponseEntity<SimulationResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.startSimulation(id));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Dừng mô phỏng")
    public ResponseEntity<SimulationResponse> stop(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.stopSimulation(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy trạng thái phiên")
    public ResponseEntity<SimulationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(journeyService.getSimulation(id));
    }
}