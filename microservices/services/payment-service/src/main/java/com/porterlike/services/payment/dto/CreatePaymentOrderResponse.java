package com.porterlike.services.payment.dto;

import java.math.BigDecimal;

public record CreatePaymentOrderResponse(
        String orderId,
        String bookingId,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String provider
) {}
