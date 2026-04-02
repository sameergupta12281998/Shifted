package com.porterlike.services.driver.dto;

public record TrackingUpdateRequest(
        String driverId,
        String bookingId,
        Double latitude,
        Double longitude
) {
}
