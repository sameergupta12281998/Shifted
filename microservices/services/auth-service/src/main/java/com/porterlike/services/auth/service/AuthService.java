package com.porterlike.services.auth.service;

import com.porterlike.services.auth.dto.AuthResponse;
import com.porterlike.services.auth.dto.LoginRequest;
import com.porterlike.services.auth.dto.RegisterRequest;
import com.porterlike.services.auth.dto.UserProfileResponse;
import com.porterlike.services.auth.model.Role;
import com.porterlike.services.auth.model.UserAccount;
import com.porterlike.services.auth.repository.UserAccountRepository;
import com.porterlike.services.auth.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("Phone already registered");
        }

        Role role = Role.valueOf(request.role().toUpperCase());
        UserAccount account = new UserAccount();
        account.setName(request.name());
        account.setPhone(request.phone());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(role);
        account.setCreatedAt(Instant.now());
        UserAccount saved = userAccountRepository.save(account);

        String token = jwtService.issueToken(saved.getId(), saved.getRole().name());
        return new AuthResponse(token, jwtService.expirySeconds(), saved.getRole().name(), saved.getId().toString());
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByPhone(request.phone())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.issueToken(account.getId(), account.getRole().name());
        return new AuthResponse(token, jwtService.expirySeconds(), account.getRole().name(), account.getId().toString());
    }

    public UserProfileResponse me(UUID userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                account.getId().toString(),
                account.getName(),
                account.getPhone(),
                account.getRole().name()
        );
    }
}
