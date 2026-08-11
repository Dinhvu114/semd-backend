package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.CreateSimulationRequest;
import com.semd.backend.dto.response.SimulationResponse;
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
        name = "Ambulance Simulation - Management",
        description = "API quản trị mô phỏng dành cho DISPATCHER / ADMIN"
)
public class SimulationManagementController {

    private final AmbulanceJourneyService journeyService;

    public SimulationManagementController(
            AmbulanceJourneyService journeyService
    ) {
        this.journeyService = journeyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "[DISPATCHER/ADMIN] Tạo phiên mô phỏng",
            description = "Tạo hành trình mô phỏng từ xe → hiện trường → bệnh viện"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> create(
            @Valid @RequestBody CreateSimulationRequest req
    ) {
        SimulationResponse result =
                journeyService.createSimulation(req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        HttpStatus.CREATED.value(),
                        "Tạo phiên mô phỏng thành công",
                        result
                ));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "[DISPATCHER/ADMIN] Bắt đầu mô phỏng"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> start(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu mô phỏng thành công",
                        journeyService.startSimulation(id)
                )
        );
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "[DISPATCHER/ADMIN] Dừng mô phỏng"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> stop(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Dừng mô phỏng thành công",
                        journeyService.stopSimulation(id)
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(
            summary = "[DISPATCHER/ADMIN] Xem trạng thái phiên mô phỏng"
    )
    public ResponseEntity<BaseResponse<SimulationResponse>> get(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        journeyService.getSimulation(id)
                )
        );
    }
}