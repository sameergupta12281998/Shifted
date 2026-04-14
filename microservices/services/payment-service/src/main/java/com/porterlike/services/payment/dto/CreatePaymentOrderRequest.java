package com.porterlike.services.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentOrderRequest(
        @NotBlank String bookingId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String method
) {}
