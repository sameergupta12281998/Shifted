package com.porterlike.services.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NearbyDriverRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String vehicleType,
        Double radiusKm,
        Integer limit
) {
    public NearbyDriverRequest {
        if (radiusKm == null) radiusKm = 5.0;
        if (limit == null) limit = 10;
    }
}
