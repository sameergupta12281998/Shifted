package com.porterlike.services.driver.dto;

public record DriverOfferResponse(
        String offerId,
        String bookingId,
        String driverId,
        String status,
        String expiresAt
) {
}
