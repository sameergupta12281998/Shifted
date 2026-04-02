package com.porterlike.services.tracking.dto;

public record TrackingSnapshot(
        String driverId,
        String bookingId,
        Double latitude,
        Double longitude,
        long updatedAtEpochMs
) {
}
