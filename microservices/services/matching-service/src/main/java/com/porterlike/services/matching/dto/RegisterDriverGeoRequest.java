package com.porterlike.services.matching.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDriverGeoRequest(
        @NotBlank String driverId,
        @NotBlank String vehicleType,
        double latitude,
        double longitude
) {}
