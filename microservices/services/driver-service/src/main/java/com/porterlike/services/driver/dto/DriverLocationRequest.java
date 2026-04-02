package com.porterlike.services.driver.dto;

import jakarta.validation.constraints.NotNull;

public record DriverLocationRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
