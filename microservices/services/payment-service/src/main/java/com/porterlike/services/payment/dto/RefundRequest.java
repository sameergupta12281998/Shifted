package com.porterlike.services.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RefundRequest(
        @NotBlank String paymentId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String reason
) {}
