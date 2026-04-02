package com.porterlike.services.booking.dto;

public record BookingResponse(
        String bookingId,
        String userId,
        String driverId,
        String pickup,
        String dropAddress,
        String vehicleType,
        String status
) {
}
