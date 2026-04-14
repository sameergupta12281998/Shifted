package com.porterlike.services.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sendReturnsAccepted() throws Exception {
        mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "user-123",
                                  "channel": "SMS",
                                  "title": "Booking Update",
                                  "message": "Driver assigned"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }
}
