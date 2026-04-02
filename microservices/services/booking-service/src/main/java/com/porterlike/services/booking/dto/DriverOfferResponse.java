package com.porterlike.services.booking.dto;

import java.time.Instant;

public record DriverOfferResponse(
        String offerId,
        String driverId,
        String bookingId,
        String status,
        Instant expiresAt
) {
}
