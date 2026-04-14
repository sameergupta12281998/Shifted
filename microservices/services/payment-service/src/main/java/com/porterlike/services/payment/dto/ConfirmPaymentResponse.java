package com.porterlike.services.payment.dto;

public record ConfirmPaymentResponse(
        String paymentId,
        String orderId,
        String status
) {}
