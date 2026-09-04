package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.PayPaymentRequest;
import com.semd.backend.dto.response.PaymentDetailResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reporter/payments")
@Tag(name = "Reporter Payment", description = "Xem và thanh toán chi phí dịch vụ")
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
    @Operation(summary = "Chi tiết chi phí theo paymentId")
    public ResponseEntity<BaseResponse<PaymentDetailResponse>> getMyPaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyPaymentById(principal.getId(), id)));
    }

    // ── THÊM MỚI ──────────────────────────────────────────
    @GetMapping("/by-call/{callId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết chi phí theo callId",
            description = "Dùng cho Mobile Reporter, tổ chức UI quanh callId")
    public ResponseEntity<BaseResponse<PaymentDetailResponse>> getMyPaymentByCallId(
            @PathVariable Integer callId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getMyPaymentByCallId(principal.getId(), callId)));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác nhận thanh toán",
            description = "Reporter xác nhận đã thanh toán qua CASH/VIETQR/VNPAY/MOMO (mô phỏng, chưa tích hợp gateway thật)")
    public ResponseEntity<BaseResponse<PaymentDetailResponse>> payPayment(
            @PathVariable Long id,
            @Valid @RequestBody PayPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.payPayment(principal.getId(), id, request.getPaymentMethod())));
    }
}