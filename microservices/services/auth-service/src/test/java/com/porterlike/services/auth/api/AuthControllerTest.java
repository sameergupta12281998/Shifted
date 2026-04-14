package com.porterlike.services.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.porterlike.services.auth.dto.AuthResponse;
import com.porterlike.services.auth.dto.UserProfileResponse;
import com.porterlike.services.auth.config.SecurityConfig;
import com.porterlike.services.auth.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void registerReturns201WhenRequestIsValid() throws Exception {
        given(authService.register(any())).willReturn(new AuthResponse("token-abc", 3600, "USER", "user-id-1"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "phone": "9876543210",
                                  "password": "secret123",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerReturns400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "phone": "abc",
                                  "password": "x",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        given(authService.login(any())).willReturn(new AuthResponse("jwt-token", 3600, "DRIVER", "driver-id-1"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "9876543210",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void loginReturns400WhenPhoneAlreadyRegistered() throws Exception {
        given(authService.login(any())).willThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "9876543210",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns400WhenPhoneAlreadyRegistered() throws Exception {
        given(authService.register(any())).willThrow(new IllegalArgumentException("Phone already registered"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bob",
                                  "phone": "9876543210",
                                  "password": "secret123",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

                  @Test
                  void meReturnsCurrentUserProfile() throws Exception {
                    UUID userId = UUID.randomUUID();
                    given(authService.me(userId)).willReturn(new UserProfileResponse(
                        userId.toString(),
                        "Alice",
                        "9876543210",
                        "USER"
                    ));

                    mockMvc.perform(get("/auth/me")
                            .header("X-Authenticated-User-Id", userId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userId").value(userId.toString()))
                        .andExpect(jsonPath("$.name").value("Alice"))
                        .andExpect(jsonPath("$.role").value("USER"));
                  }
}
