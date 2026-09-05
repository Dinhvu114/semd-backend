package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.PaymentDetailResponse;
import com.semd.backend.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatcher/payments")
@Tag(name = "Dispatcher Payment", description = "Dispatcher xem chi phí phát sinh của request")
public class DispatcherPaymentController {

    private final PaymentQueryService paymentQueryService;

    public DispatcherPaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping("/by-request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Xem chi phí theo requestId",
            description = "Dùng cho Web Dispatcher hiển thị trạng thái thanh toán thật, "
                    + "thay vì suy diễn từ trạng thái Request")
    public ResponseEntity<BaseResponse<PaymentDetailResponse>> getPaymentByRequestId(
            @PathVariable Integer requestId) {
        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getPaymentByRequestId(requestId)));
    }
}