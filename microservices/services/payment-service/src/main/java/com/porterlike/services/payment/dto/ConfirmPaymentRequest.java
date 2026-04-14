package com.porterlike.services.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank String orderId,
        String providerRef
) {}
