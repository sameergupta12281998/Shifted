package com.porterlike.services.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BookingFraudCheckRequest(
        @NotBlank String bookingId,
        @NotBlank String userId,
        @NotNull Double pickupLatitude,
        @NotNull Double pickupLongitude,
        @NotNull Double dropLatitude,
        @NotNull Double dropLongitude,
        BigDecimal estimatedFare,
        String vehicleType
) {}
