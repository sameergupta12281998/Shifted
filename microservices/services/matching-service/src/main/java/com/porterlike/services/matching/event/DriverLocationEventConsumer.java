package com.porterlike.services.matching.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.matching.dto.RegisterDriverGeoRequest;
import com.porterlike.services.matching.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to driver location updates published by driver-service so the
 * matching geo-index stays current without polling.
 */
@Component
public class DriverLocationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationEventConsumer.class);

    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    public DriverLocationEventConsumer(MatchingService matchingService, ObjectMapper objectMapper) {
        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "driver.location.updated", groupId = "matching-service")
    public void onDriverLocationUpdated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            RegisterDriverGeoRequest req = new RegisterDriverGeoRequest(
                    node.get("driverId").asText(),
                    node.get("vehicleType").asText(),
                    node.get("latitude").asDouble(),
                    node.get("longitude").asDouble()
            );
            matchingService.registerDriverLocation(req);
        } catch (Exception e) {
            log.warn("Failed to process driver.location.updated event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking.assigned", groupId = "matching-service")
    public void onBookingAssigned(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            matchingService.markUnavailable(
                    node.get("driverId").asText(),
                    node.get("vehicleType").asText()
            );
        } catch (Exception e) {
            log.warn("Failed to process booking.assigned event: {}", e.getMessage());
        }
    }
}
