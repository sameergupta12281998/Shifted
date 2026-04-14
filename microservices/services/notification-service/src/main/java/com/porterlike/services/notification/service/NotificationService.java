package com.porterlike.services.notification.service;

import com.porterlike.services.notification.dto.SendNotificationRequest;
import com.porterlike.services.notification.dto.SendNotificationResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final String provider;

    public NotificationService(@Value("${app.notification.provider:stub}") String provider) {
        this.provider = provider;
    }

    public SendNotificationResponse send(SendNotificationRequest request) {
        return new SendNotificationResponse(
                UUID.randomUUID().toString(),
                "QUEUED",
                provider
        );
    }
}
