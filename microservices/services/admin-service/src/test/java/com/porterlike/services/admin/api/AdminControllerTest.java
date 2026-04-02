package com.porterlike.services.admin.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.porterlike.services.admin.config.RestClientConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(AdminController.class)
@Import({GlobalExceptionHandler.class, RestClientConfig.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void pingReturnsOkForAdmin() throws Exception {
        mockMvc.perform(get("/admin/ping")
                        .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void pingReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/ping")
                        .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pingReturnsBadRequestWhenAuthHeadersMissing() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isBadRequest());
    }
}
