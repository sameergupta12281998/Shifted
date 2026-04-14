package com.porterlike.services.driver.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.porterlike.services.driver.dto.DriverResponse;
import com.porterlike.services.driver.service.DriverService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DriverController.class)
@Import(GlobalExceptionHandler.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverService driverService;

    @Test
    void registerReturnsCreatedWhenAuthorized() throws Exception {
        given(driverService.register(any(), any())).willReturn(new DriverResponse(
                UUID.randomUUID().toString(),
                "Raj",
                "BIKE",
                "KA01AB1234",
                "UNVERIFIED",
                false,
                true,
                null,
                null,
                null
        ));

        mockMvc.perform(post("/driver/register")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Role", "DRIVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Raj",
                                  "vehicleType": "BIKE",
                                  "vehicleNumber": "KA01AB1234"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void setOnlineReturnsForbiddenWhenServiceRejectsAccess() throws Exception {
        UUID driverId = UUID.randomUUID();
        given(driverService.setOnline(any(), eq(driverId), eq(true))).willThrow(new SecurityException("Driver access denied"));

        mockMvc.perform(post("/driver/{id}/online", driverId)
                        .param("online", "true")
                        .header("X-Authenticated-User-Id", UUID.randomUUID())
                        .header("X-Authenticated-Role", "DRIVER"))
                .andExpect(status().isForbidden());
    }
}