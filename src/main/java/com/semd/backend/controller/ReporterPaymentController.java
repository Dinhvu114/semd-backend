package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.PaymentDetailResponse;
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
@RequestMapping("/api/v1/reporter/payments")
@Tag(name = "Reporter Payment", description = "Xem chi phí dịch vụ cần thanh toán")
public class ReporterPaymentController {

    private final PaymentQueryService paymentQueryService;

    public ReporterPaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách chi phí các ca cấp cứu của tôi")
    public ResponseEntity<BaseResponse<List<PaymentDetailResponse>>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyPayments(principal.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết chi phí một ca cấp cứu")
    public ResponseEntity<BaseResponse<PaymentDetailResponse>> getMyPaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyPaymentById(principal.getId(), id)));
    }
}