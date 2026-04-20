package com.porterlike.services.matching.dto;

public record DriverCandidate(
        String driverId,
        String vehicleType,
        double latitude,
        double longitude,
        double distanceKm,
        double score
) {}
