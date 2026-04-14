package com.porterlike.services.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record SendNotificationRequest(
        @NotBlank String recipientId,
        @NotBlank String channel,
        @NotBlank String title,
        @NotBlank String message
) {
}
