package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.DriverEarningResponse;
import com.semd.backend.dto.response.DriverEarningSummaryResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver/earnings")
@Tag(name = "Driver Earning", description = "Xem thu nhập dự kiến của tài xế")
public class DriverEarningController {

    private final PaymentQueryService paymentQueryService;

    public DriverEarningController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Danh sách thu nhập theo từng nhiệm vụ")
    public ResponseEntity<BaseResponse<List<DriverEarningResponse>>> getMyEarnings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyEarnings(principal.getId())));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Tổng hợp thu nhập dự kiến")
    public ResponseEntity<BaseResponse<DriverEarningSummaryResponse>> getMyEarningSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyEarningSummary(principal.getId())));
    }
}