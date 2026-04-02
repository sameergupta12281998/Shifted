package com.porterlike.services.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.tracking.dto.TrackingSnapshot;
import com.porterlike.services.tracking.dto.TrackingUpdateRequest;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {

    private static final String PREFIX = "driver:location:";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public TrackingService(
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public TrackingSnapshot update(TrackingUpdateRequest request) {
        TrackingSnapshot snapshot = new TrackingSnapshot(
                request.driverId(),
                request.bookingId(),
                request.latitude(),
                request.longitude(),
                Instant.now().toEpochMilli()
        );

        try {
            redisTemplate.opsForValue().set(PREFIX + request.driverId(), objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize tracking snapshot");
        }

        messagingTemplate.convertAndSend("/topic/tracking/" + request.driverId(), snapshot);
        return snapshot;
    }

    public Optional<TrackingSnapshot> getByDriver(String driverId) {
        String data = redisTemplate.opsForValue().get(PREFIX + driverId);
        if (data == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(data, TrackingSnapshot.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
