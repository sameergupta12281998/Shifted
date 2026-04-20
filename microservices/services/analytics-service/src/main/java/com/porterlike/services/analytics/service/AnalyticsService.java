package com.porterlike.services.analytics.service;

import com.porterlike.services.analytics.dto.AnalyticsSummary;
import com.porterlike.services.analytics.model.DomainEvent;
import com.porterlike.services.analytics.repository.DomainEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final DomainEventRepository repository;

    public AnalyticsService(DomainEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordEvent(String eventType, String sourceId, BigDecimal amount,
                            String vehicleType, String driverId, String rawPayload) {
        DomainEvent event = new DomainEvent();
        event.setEventType(eventType);
        event.setSourceId(sourceId);
        event.setAmount(amount);
        event.setVehicleType(vehicleType);
        event.setDriverId(driverId);
        event.setPayload(rawPayload);
        event.setReceivedAt(Instant.now());
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary getSummary() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long totalBookings = repository.countByEventType("booking.created");
        long bookingsLast24h = repository.countByEventTypeSince("booking.created", since24h);
        long completed = repository.countByEventType("booking.completed");
        long cancelled = repository.countByEventType("booking.cancelled");

        BigDecimal revenueTotal = repository.sumAmountByEventTypeSince("payment.completed", Instant.EPOCH);
        BigDecimal revenueLast24h = repository.sumAmountByEventTypeSince("payment.completed", since24h);

        long activeDrivers = repository.countActiveDriversSince(since24h);
        long totalPayments = repository.countByEventType("payment.completed");

        return new AnalyticsSummary(
                totalBookings, bookingsLast24h, completed, cancelled,
                revenueTotal, revenueLast24h, activeDrivers, totalPayments
        );
    }
}
