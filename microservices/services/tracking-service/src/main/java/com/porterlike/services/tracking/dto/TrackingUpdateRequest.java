package com.porterlike.services.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrackingUpdateRequest(
        @NotBlank String driverId,
        String bookingId,
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
