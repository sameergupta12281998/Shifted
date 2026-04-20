package com.porterlike.services.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka topics.
 * All events are serialized as JSON strings.
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishBookingCreated(String bookingId, String userId, String vehicleType,
                                      String pickup, String dropAddress) {
        publish("booking.created", Map.of(
                "bookingId", bookingId,
                "userId", userId,
                "vehicleType", vehicleType,
                "pickup", pickup,
                "dropAddress", dropAddress
        ));
    }

    public void publishBookingAssigned(String bookingId, String driverId, String vehicleType) {
        publish("booking.assigned", Map.of(
                "bookingId", bookingId,
                "driverId", driverId,
                "vehicleType", vehicleType
        ));
    }

    public void publishBookingCompleted(String bookingId, String userId, String driverId, String vehicleType) {
        publish("booking.completed", Map.of(
                "bookingId", bookingId,
                "userId", userId,
                "driverId", driverId,
                "vehicleType", vehicleType
        ));
    }

    public void publishBookingCancelled(String bookingId, String userId) {
        publish("booking.cancelled", Map.of(
                "bookingId", bookingId,
                "userId", userId
        ));
    }

    private void publish(String topic, Map<String, ?> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            log.warn("Failed to publish event to topic [{}]: {}", topic, e.getMessage());
        }
    }
}
