package com.porterlike.services.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBookingRequest(
        @NotBlank String pickup,
        @NotBlank String dropAddress,
        @NotBlank String vehicleType
) {
}
