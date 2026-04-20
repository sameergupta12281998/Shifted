package com.porterlike.services.rating.dto;

import java.util.UUID;

public record RatingStats(
        UUID subjectId,
        String roleTarget,
        double averageScore,
        long totalRatings
) {}
