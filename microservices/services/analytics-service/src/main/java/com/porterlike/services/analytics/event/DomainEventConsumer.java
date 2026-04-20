package com.porterlike.services.analytics.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.analytics.service.AnalyticsService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes all domain events from Kafka and persists them for analytics queries.
 * Each topic maps to a specific event type stored in the domain_events table.
 */
@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public DomainEventConsumer(AnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "booking.created", groupId = "analytics-service")
    public void onBookingCreated(String payload) {
        ingest("booking.created", payload, "bookingId", null, "vehicleType", null);
    }

    @KafkaListener(topics = "booking.completed", groupId = "analytics-service")
    public void onBookingCompleted(String payload) {
        ingest("booking.completed", payload, "bookingId", null, "vehicleType", "driverId");
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "analytics-service")
    public void onBookingCancelled(String payload) {
        ingest("booking.cancelled", payload, "bookingId", null, null, null);
    }

    @KafkaListener(topics = "booking.assigned", groupId = "analytics-service")
    public void onBookingAssigned(String payload) {
        ingest("booking.assigned", payload, "bookingId", null, "vehicleType", "driverId");
    }

    @KafkaListener(topics = "payment.completed", groupId = "analytics-service")
    public void onPaymentCompleted(String payload) {
        ingest("payment.completed", payload, "bookingId", "amount", "vehicleType", null);
    }

    @KafkaListener(topics = "driver.location.updated", groupId = "analytics-service")
    public void onDriverLocationUpdated(String payload) {
        ingest("driver.location.updated", payload, null, null, "vehicleType", "driverId");
    }

    @KafkaListener(topics = "driver.registered", groupId = "analytics-service")
    public void onDriverRegistered(String payload) {
        ingest("driver.registered", payload, "driverId", null, "vehicleType", "driverId");
    }

    @KafkaListener(topics = "user.registered", groupId = "analytics-service")
    public void onUserRegistered(String payload) {
        ingest("user.registered", payload, "userId", null, null, null);
    }

    private void ingest(String eventType, String rawPayload,
                        String sourceIdField, String amountField,
                        String vehicleTypeField, String driverIdField) {
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            String sourceId = sourceIdField != null && node.has(sourceIdField)
                    ? node.get(sourceIdField).asText() : null;
            BigDecimal amount = amountField != null && node.has(amountField)
                    ? new BigDecimal(node.get(amountField).asText()) : null;
            String vehicleType = vehicleTypeField != null && node.has(vehicleTypeField)
                    ? node.get(vehicleTypeField).asText() : null;
            String driverId = driverIdField != null && node.has(driverIdField)
                    ? node.get(driverIdField).asText() : null;
            analyticsService.recordEvent(eventType, sourceId, amount, vehicleType, driverId, rawPayload);
        } catch (Exception e) {
            log.warn("Failed to ingest event [{}]: {}", eventType, e.getMessage());
        }
    }
}
