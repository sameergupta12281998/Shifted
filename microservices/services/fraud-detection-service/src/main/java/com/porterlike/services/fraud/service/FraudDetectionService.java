package com.porterlike.services.fraud.service;

import com.porterlike.services.fraud.dto.BookingFraudCheckRequest;
import com.porterlike.services.fraud.dto.FraudCheckResponse;
import com.porterlike.services.fraud.dto.PaymentFraudCheckRequest;
import com.porterlike.services.fraud.model.FraudReport;
import com.porterlike.services.fraud.model.FraudReport.RiskLevel;
import com.porterlike.services.fraud.repository.FraudReportRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudDetectionService {

    private final FraudReportRepository reportRepository;
    private final int maxBookingsPerHour;
    private final BigDecimal maxPaymentAmount;

    public FraudDetectionService(
            FraudReportRepository reportRepository,
            @Value("${app.fraud.max-bookings-per-hour:10}") int maxBookingsPerHour,
            @Value("${app.fraud.max-payment-amount:50000}") BigDecimal maxPaymentAmount
    ) {
        this.reportRepository = reportRepository;
        this.maxBookingsPerHour = maxBookingsPerHour;
        this.maxPaymentAmount = maxPaymentAmount;
    }

    /**
     * Rule-based booking fraud check:
     * 1. Unrealistic route: pickup == drop within 50 m
     * 2. Rate abuse: user placed more than maxBookingsPerHour in last 60 min
     * 3. GPS spoofing signal: coordinates at (0,0) or outside India bounding box
     */
    @Transactional
    public FraudCheckResponse checkBooking(BookingFraudCheckRequest request) {
        // Rule 1: same-location booking
        double distance = haversineKm(
                request.pickupLatitude(), request.pickupLongitude(),
                request.dropLatitude(), request.dropLongitude()
        );
        if (distance < 0.05) {
            return flag(request.bookingId(), "BOOKING", RiskLevel.HIGH,
                    "SAME_LOCATION", "Pickup and drop are within 50 m");
        }

        // Rule 2: rate abuse
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = reportRepository.countBySubjectIdAndCreatedAtAfter(request.userId(), oneHourAgo);
        if (recentCount >= maxBookingsPerHour) {
            return flag(request.bookingId(), "BOOKING", RiskLevel.CRITICAL,
                    "RATE_ABUSE", "User exceeded " + maxBookingsPerHour + " bookings/hour");
        }

        // Rule 3: GPS outside India bounding box or null-island
        if (!isWithinIndia(request.pickupLatitude(), request.pickupLongitude())
                || !isWithinIndia(request.dropLatitude(), request.dropLongitude())) {
            return flag(request.bookingId(), "BOOKING", RiskLevel.MEDIUM,
                    "GPS_OUTSIDE_REGION", "Coordinates outside expected service region");
        }

        return clean(request.bookingId());
    }

    /**
     * Rule-based payment fraud check:
     * 1. Unusually large payment amount
     * 2. User with multiple recent HIGH-risk fraud reports
     */
    @Transactional
    public FraudCheckResponse checkPayment(PaymentFraudCheckRequest request) {
        // Rule 1: large amount
        if (request.amount().compareTo(maxPaymentAmount) > 0) {
            return flag(request.paymentId(), "PAYMENT", RiskLevel.HIGH,
                    "LARGE_AMOUNT", "Payment amount exceeds threshold of " + maxPaymentAmount);
        }

        // Rule 2: user previously flagged as HIGH/CRITICAL
        long priorFlags = reportRepository.countBySubjectIdAndCreatedAtAfter(
                request.userId(), Instant.now().minus(24, ChronoUnit.HOURS));
        if (priorFlags >= 3) {
            return flag(request.paymentId(), "PAYMENT", RiskLevel.CRITICAL,
                    "REPEAT_OFFENDER", "User has " + priorFlags + " fraud signals in last 24h");
        }

        return clean(request.paymentId());
    }

    @Transactional(readOnly = true)
    public List<FraudReport> getPendingReports() {
        return reportRepository.findByReviewedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public FraudReport markReviewed(java.util.UUID reportId) {
        FraudReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.setReviewed(true);
        return reportRepository.save(report);
    }

    private FraudCheckResponse flag(String subjectId, String subjectType,
                                    RiskLevel level, String rule, String reason) {
        FraudReport report = new FraudReport();
        report.setSubjectId(subjectId);
        report.setSubjectType(subjectType);
        report.setRiskLevel(level);
        report.setRuleTriggered(rule);
        report.setReason(reason);
        report.setCreatedAt(Instant.now());
        reportRepository.save(report);
        return new FraudCheckResponse(subjectId, level.name(), true, reason);
    }

    private FraudCheckResponse clean(String subjectId) {
        return new FraudCheckResponse(subjectId, "LOW", false, "No fraud signals detected");
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static boolean isWithinIndia(double lat, double lon) {
        return lat >= 6.0 && lat <= 38.0 && lon >= 68.0 && lon <= 98.0;
    }
}
