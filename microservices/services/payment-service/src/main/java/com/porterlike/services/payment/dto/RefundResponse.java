package com.porterlike.services.payment.dto;

import java.math.BigDecimal;

public record RefundResponse(
        String refundId,
        String paymentId,
        BigDecimal amount,
        String status
) {}
