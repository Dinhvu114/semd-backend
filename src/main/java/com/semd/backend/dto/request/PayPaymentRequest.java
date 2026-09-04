package com.semd.backend.dto.request;

import com.semd.backend.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PayPaymentRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}