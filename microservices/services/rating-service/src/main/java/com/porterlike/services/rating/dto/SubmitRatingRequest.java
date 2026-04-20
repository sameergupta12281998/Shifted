package com.porterlike.services.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitRatingRequest(
        @NotNull java.util.UUID bookingId,
        @NotNull java.util.UUID fromUserId,
        @NotNull java.util.UUID toUserId,
        @NotBlank String roleTarget,
        @Min(1) @Max(5) int score,
        String comment
) {}
