package com.porterlike.services.fraud.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.fraud.dto.BookingFraudCheckRequest;
import com.porterlike.services.fraud.service.FraudDetectionService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Automatically triggers fraud checks when bookings are created via Kafka.
 */
@Component
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final FraudDetectionService fraudDetectionService;
    private final ObjectMapper objectMapper;

    public BookingEventConsumer(FraudDetectionService fraudDetectionService, ObjectMapper objectMapper) {
        this.fraudDetectionService = fraudDetectionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "booking.created", groupId = "fraud-detection-service")
    public void onBookingCreated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            BookingFraudCheckRequest req = new BookingFraudCheckRequest(
                    node.get("bookingId").asText(),
                    node.get("userId").asText(),
                    node.has("pickupLatitude") ? node.get("pickupLatitude").asDouble() : 12.9716,
                    node.has("pickupLongitude") ? node.get("pickupLongitude").asDouble() : 77.5946,
                    node.has("dropLatitude") ? node.get("dropLatitude").asDouble() : 12.9716,
                    node.has("dropLongitude") ? node.get("dropLongitude").asDouble() : 77.5946,
                    node.has("estimatedFare") ? new BigDecimal(node.get("estimatedFare").asText()) : null,
                    node.has("vehicleType") ? node.get("vehicleType").asText() : null
            );
            var result = fraudDetectionService.checkBooking(req);
            if (result.flagged()) {
                log.warn("FRAUD ALERT booking={} risk={} reason={}", req.bookingId(), result.riskLevel(), result.reason());
            }
        } catch (Exception e) {
            log.warn("Failed to process booking.created for fraud check: {}", e.getMessage());
        }
    }
}
