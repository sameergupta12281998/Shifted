package com.porterlike.services.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.notification.dto.SendNotificationRequest;
import com.porterlike.services.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events and triggers notifications automatically.
 * Eliminates the need for upstream services to call the notification REST API.
 */
@Component
public class BookingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public BookingEventConsumer(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "booking.created", groupId = "notification-service")
    public void onBookingCreated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String userId = node.get("userId").asText();
            String bookingId = node.get("bookingId").asText();
            notificationService.send(new SendNotificationRequest(
                    userId, "SMS",
                    "Booking Created",
                    "Your booking " + bookingId + " has been created. Finding a driver..."
            ));
        } catch (Exception e) {
            log.warn("Failed to handle booking.created for notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking.assigned", groupId = "notification-service")
    public void onBookingAssigned(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String bookingId = node.get("bookingId").asText();
            String driverId = node.get("driverId").asText();
            notificationService.send(new SendNotificationRequest(
                    driverId, "PUSH",
                    "New Booking",
                    "You have a new booking " + bookingId + ". Please head to the pickup point."
            ));
        } catch (Exception e) {
            log.warn("Failed to handle booking.assigned for notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking.completed", groupId = "notification-service")
    public void onBookingCompleted(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String userId = node.get("userId").asText();
            String bookingId = node.get("bookingId").asText();
            notificationService.send(new SendNotificationRequest(
                    userId, "SMS",
                    "Booking Completed",
                    "Your booking " + bookingId + " is complete. Please rate your driver."
            ));
        } catch (Exception e) {
            log.warn("Failed to handle booking.completed for notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "notification-service")
    public void onBookingCancelled(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String userId = node.get("userId").asText();
            String bookingId = node.get("bookingId").asText();
            notificationService.send(new SendNotificationRequest(
                    userId, "SMS",
                    "Booking Cancelled",
                    "Your booking " + bookingId + " has been cancelled."
            ));
        } catch (Exception e) {
            log.warn("Failed to handle booking.cancelled for notification: {}", e.getMessage());
        }
    }
}
