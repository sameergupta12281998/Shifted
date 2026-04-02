package com.porterlike.services.booking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.porterlike.services.booking.dto.BookingResponse;
import com.porterlike.services.booking.service.BookingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void createRequiresIdempotencyHeader() throws Exception {
        mockMvc.perform(post("/booking/create")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pickup": "A",
                                  "dropAddress": "B",
                                  "vehicleType": "BIKE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsForbiddenWhenServiceRejectsAccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        given(bookingService.get(any(), eq(bookingId))).willThrow(new SecurityException("Booking access denied"));

        mockMvc.perform(get("/booking/{id}", bookingId)
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnsCreatedWhenAuthorized() throws Exception {
        given(bookingService.create(any(), eq("req-1"), any())).willReturn(new BookingResponse(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                null,
                "A",
                "B",
                "BIKE",
                "CREATED"
        ));

        mockMvc.perform(post("/booking/create")
                        .header("Idempotency-Key", "req-1")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pickup": "A",
                                  "dropAddress": "B",
                                  "vehicleType": "BIKE"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}