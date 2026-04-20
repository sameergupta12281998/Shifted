package com.porterlike.services.rating.dto;

import java.util.UUID;

public record RatingResponse(
        UUID id,
        UUID bookingId,
        UUID fromUserId,
        UUID toUserId,
        String roleTarget,
        int score,
        String comment
) {}
