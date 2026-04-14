package com.porterlike.services.pricing.dto;

public record PricingEstimateResponse(
        String vehicleType,
        double distanceKm,
        double durationMinutes,
        double baseFare,
        double distanceFare,
        double timeFare,
        double totalFare,
        String currency
) {
}
