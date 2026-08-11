package com.semd.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.RejectMissionRequest;
import com.semd.backend.dto.response.DispatchMissionResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.DispatchMissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/dispatch-missions")
@Tag(
        name = "DRIVER - Mission",
        description = "API nhiệm vụ dành cho tài xế"
)
public class DriverMissionController {

    private final DispatchMissionService missionService;

    public DriverMissionController(
            DispatchMissionService missionService
    ) {
        this.missionService = missionService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Lịch sử nhiệm vụ của tài xế đang đăng nhập")
    public ResponseEntity<BaseResponse<List<DispatchMissionResponse>>> getMyMissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        missionService.getMyMissions(principal.getId())
                )
        );
    }

    @GetMapping("/me/active")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Các nhiệm vụ đang hoạt động của tài xế")
    public ResponseEntity<BaseResponse<List<DispatchMissionResponse>>> getMyActiveMissions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        missionService.getMyActiveMissions(principal.getId())
                )
        );
    }

    @GetMapping("/me/{missionId}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Chi tiết nhiệm vụ của tài xế")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> getMyMission(
            @PathVariable Integer missionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        missionService.getMyMission(
                                principal.getId(),
                                missionId
                        )
                )
        );
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Chấp nhận nhiệm vụ")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> accept(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Nhận nhiệm vụ thành công",
                        missionService.accept(id)
                )
        );
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Từ chối nhiệm vụ")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> reject(
            @PathVariable Integer id,
            @Valid @RequestBody RejectMissionRequest request
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Từ chối nhiệm vụ thành công",
                        missionService.reject(id, request)
                )
        );
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Bắt đầu di chuyển đến hiện trường")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> start(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu di chuyển đến hiện trường",
                        missionService.start(id)
                )
        );
    }

    @PostMapping("/{id}/arrive-scene")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Xác nhận đã đến hiện trường")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveScene(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Đã cập nhật trạng thái đến hiện trường",
                        missionService.arriveScene(id)
                )
        );
    }

    @PostMapping("/{id}/start-transport")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Bắt đầu vận chuyển bệnh nhân")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> startTransport(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Bắt đầu vận chuyển bệnh nhân",
                        missionService.startTransport(id)
                )
        );
    }

    @PostMapping("/{id}/arrive-hospital")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Xác nhận đã đến bệnh viện")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> arriveHospital(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Đã cập nhật trạng thái đến bệnh viện",
                        missionService.arriveHospital(id)
                )
        );
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Hoàn thành nhiệm vụ")
    public ResponseEntity<BaseResponse<DispatchMissionResponse>> complete(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                BaseResponse.success(
                        HttpStatus.OK.value(),
                        "Hoàn thành nhiệm vụ thành công",
                        missionService.complete(id)
                )
        );
    }
}
