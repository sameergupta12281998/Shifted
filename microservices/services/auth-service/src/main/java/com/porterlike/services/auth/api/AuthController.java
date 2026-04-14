package com.porterlike.services.auth.api;

import com.porterlike.services.auth.dto.AuthResponse;
import com.porterlike.services.auth.dto.LoginRequest;
import com.porterlike.services.auth.dto.RegisterRequest;
import com.porterlike.services.auth.dto.UserProfileResponse;
import com.porterlike.services.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader("X-Authenticated-User-Id") String principalId) {
        return authService.me(UUID.fromString(principalId));
    }
}
