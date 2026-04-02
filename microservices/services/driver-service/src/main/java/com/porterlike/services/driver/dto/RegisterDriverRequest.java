package com.porterlike.services.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDriverRequest(
        @NotBlank String name,
        @NotBlank String vehicleType,
        @NotBlank String vehicleNumber
) {
}
