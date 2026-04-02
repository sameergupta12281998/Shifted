package com.porterlike.services.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.porterlike.services.tracking.dto.TrackingSnapshot;
import com.porterlike.services.tracking.dto.TrackingUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @Test
    void updateStoresSnapshotInRedisAndReturnsIt() {
        TrackingUpdateRequest request = new TrackingUpdateRequest("driver-1", "booking-1", 12.97, 77.59);

        TrackingSnapshot result = trackingService.update(request);

        assertThat(result.driverId()).isEqualTo("driver-1");
        assertThat(result.bookingId()).isEqualTo("booking-1");
        assertThat(result.latitude()).isEqualTo(12.97);
        assertThat(result.longitude()).isEqualTo(77.59);
        assertThat(result.updatedAtEpochMs()).isPositive();

        verify(valueOps).set(eq("driver:location:driver-1"), any(String.class));
    }

    @Test
    void updatePublishesSnapshotToWebSocketTopic() {
        TrackingUpdateRequest request = new TrackingUpdateRequest("driver-2", null, 13.0, 78.0);

        trackingService.update(request);

        verify(messagingTemplate).convertAndSend(eq("/topic/tracking/driver-2"), any(TrackingSnapshot.class));
    }

    @Test
    void getByDriverReturnsSnapshotWhenFoundInRedis() throws Exception {
        TrackingSnapshot stored = new TrackingSnapshot("driver-1", "booking-1", 12.97, 77.59, 1000L);
        String json = new ObjectMapper().writeValueAsString(stored);
        given(valueOps.get("driver:location:driver-1")).willReturn(json);

        Optional<TrackingSnapshot> result = trackingService.getByDriver("driver-1");

        assertThat(result).isPresent();
        assertThat(result.get().driverId()).isEqualTo("driver-1");
        assertThat(result.get().latitude()).isEqualTo(12.97);
    }

    @Test
    void getByDriverReturnsEmptyWhenKeyNotInRedis() {
        given(valueOps.get("driver:location:unknown")).willReturn(null);

        Optional<TrackingSnapshot> result = trackingService.getByDriver("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void getByDriverReturnsEmptyWhenRedisValueIsCorrupted() {
        given(valueOps.get("driver:location:bad")).willReturn("not-valid-json{{{");

        Optional<TrackingSnapshot> result = trackingService.getByDriver("bad");

        assertThat(result).isEmpty();
    }
}
