package com.porterlike.services.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record PricingEstimateRequest(
        @NotBlank String vehicleType,
        @DecimalMin("0.1") double distanceKm,
        @DecimalMin("0.0") double durationMinutes
) {
}
