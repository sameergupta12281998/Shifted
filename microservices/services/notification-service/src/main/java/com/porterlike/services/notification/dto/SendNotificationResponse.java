package com.porterlike.services.notification.dto;

public record SendNotificationResponse(
        String notificationId,
        String status,
        String provider
) {
}
