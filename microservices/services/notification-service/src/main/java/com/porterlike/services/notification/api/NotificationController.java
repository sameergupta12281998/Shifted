package com.porterlike.services.notification.api;

import com.porterlike.services.notification.dto.SendNotificationRequest;
import com.porterlike.services.notification.dto.SendNotificationResponse;
import com.porterlike.services.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SendNotificationResponse send(@Valid @RequestBody SendNotificationRequest request) {
        return notificationService.send(request);
    }
}
