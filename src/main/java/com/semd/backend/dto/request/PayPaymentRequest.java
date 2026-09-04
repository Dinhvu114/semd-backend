package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PayPaymentRequest {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // CASH, VIETQR, VNPAY, MOMO

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}