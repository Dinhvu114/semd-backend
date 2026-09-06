package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.AdminPaymentResponse;
import com.semd.backend.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payments")
@Tag(name = "Admin Payment", description = "Admin xem toàn bộ giao dịch, kể cả phần 10% platform")
public class AdminPaymentController {

    private final PaymentQueryService paymentQueryService;

    public AdminPaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách toàn bộ giao dịch",
            description = "Filter theo status (PENDING/SUCCESS/REFUNDED), không truyền = lấy tất cả")
    public ResponseEntity<BaseResponse<List<AdminPaymentResponse>>> getAllPayments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getAllPaymentsForAdmin(status)));
    }
}