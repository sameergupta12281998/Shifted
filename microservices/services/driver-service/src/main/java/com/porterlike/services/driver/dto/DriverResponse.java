package com.porterlike.services.driver.dto;

public record DriverResponse(
        String id,
        String name,
        String vehicleType,
        String vehicleNumber,
        boolean online,
        boolean available,
        String currentBookingId,
        Double latitude,
        Double longitude
) {
}
