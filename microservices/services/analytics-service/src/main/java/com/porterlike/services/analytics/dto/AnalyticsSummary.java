package com.porterlike.services.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsSummary(
        long totalBookings,
        long bookingsLast24h,
        long completedBookings,
        long cancelledBookings,
        BigDecimal revenueTotal,
        BigDecimal revenueLast24h,
        long activeDriversLast24h,
        long totalPaymentsCompleted
) {}
