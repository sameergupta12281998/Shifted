package com.porterlike.services.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentFraudCheckRequest(
        @NotBlank String paymentId,
        @NotBlank String userId,
        @NotNull BigDecimal amount,
        String method,
        String ipAddress
) {}
