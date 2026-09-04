package com.semd.backend.controller;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.response.ProviderPaymentResponse;
import com.semd.backend.entity.User;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.repository.UserRepository;
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
@RequestMapping("/api/v1/provider/payments")
@Tag(name = "Provider Payment", description = "Xem doanh thu của nhà cung cấp")
public class ProviderPaymentController {

    private final PaymentQueryService paymentQueryService;
    private final UserRepository userRepository;

    public ProviderPaymentController(PaymentQueryService paymentQueryService,
                                     UserRepository userRepository) {
        this.paymentQueryService = paymentQueryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    @Operation(summary = "Danh sách giao dịch của Provider hiện tại",
            description = "providerId tự lấy từ account đăng nhập, không nhận từ query param")
    public ResponseEntity<BaseResponse<List<ProviderPaymentResponse>>> getProviderPayments(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessConflictException("Không tìm thấy người dùng"));

        if (user.getProvider() == null) {
            throw new BusinessConflictException("Tài khoản chưa gắn với Provider nào");
        }

        return ResponseEntity.ok(BaseResponse.success(
                paymentQueryService.getProviderPayments(user.getProvider().getId())));
    }
}