package com.porterlike.services.tracking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.porterlike.services.tracking.dto.TrackingSnapshot;
import com.porterlike.services.tracking.service.TrackingService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrackingController.class)
@Import(GlobalExceptionHandler.class)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    @Value("${app.security.internal-service-token}")
    private String internalServiceToken;

    @Test
    void updateLocationReturns202WithSnapshot() throws Exception {
        TrackingSnapshot snapshot = new TrackingSnapshot("driver-1", "booking-1", 12.97, 77.59, System.currentTimeMillis());
        given(trackingService.update(any())).willReturn(snapshot);

        mockMvc.perform(post("/tracking/location")
            .header("X-Internal-Service-Token", internalServiceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": "driver-1",
                                  "bookingId": "booking-1",
                                  "latitude": 12.97,
                                  "longitude": 77.59
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.driverId").value("driver-1"))
                .andExpect(jsonPath("$.latitude").value(12.97));
    }

    @Test
    void updateLocationReturns400WhenDriverIdBlank() throws Exception {
        mockMvc.perform(post("/tracking/location")
              .header("X-Internal-Service-Token", internalServiceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": "",
                                  "latitude": 12.97,
                                  "longitude": 77.59
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLocationReturns403WithoutInternalToken() throws Exception {
        mockMvc.perform(post("/tracking/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "driverId": "driver-1",
                                  "latitude": 12.97,
                                  "longitude": 77.59
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDriverLocationReturns200WhenFound() throws Exception {
        TrackingSnapshot snapshot = new TrackingSnapshot("driver-1", null, 12.97, 77.59, 1000L);
        given(trackingService.getByDriver("driver-1")).willReturn(Optional.of(snapshot));

        mockMvc.perform(get("/tracking/driver/driver-1")
            .header("X-Authenticated-User-Id", "user-1")
            .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-1"))
                .andExpect(jsonPath("$.longitude").value(77.59));
    }

    @Test
    void getDriverLocationReturns404WhenNotFound() throws Exception {
        given(trackingService.getByDriver("unknown-driver")).willReturn(Optional.empty());

        mockMvc.perform(get("/tracking/driver/unknown-driver")
                .header("X-Authenticated-User-Id", "user-1")
                .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isNotFound());
    }

      @Test
      void getDriverLocationReturns403WithoutAuthenticatedHeaders() throws Exception {
        mockMvc.perform(get("/tracking/driver/driver-1"))
            .andExpect(status().isForbidden());
      }
}
