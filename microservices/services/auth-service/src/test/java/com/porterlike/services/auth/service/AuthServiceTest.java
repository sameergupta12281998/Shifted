package com.porterlike.services.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.porterlike.services.auth.dto.AuthResponse;
import com.porterlike.services.auth.dto.LoginRequest;
import com.porterlike.services.auth.dto.RegisterRequest;
import com.porterlike.services.auth.dto.UserProfileResponse;
import com.porterlike.services.auth.model.Role;
import com.porterlike.services.auth.model.UserAccount;
import com.porterlike.services.auth.repository.UserAccountRepository;
import com.porterlike.services.auth.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UserAccount savedUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        savedUser = new UserAccount();
        savedUser.setName("Alice");
        savedUser.setPhone("9876543210");
        savedUser.setPasswordHash("hashed-password");
        savedUser.setRole(Role.USER);
        ReflectionTestUtils.setField(savedUser, "id", userId);
    }

    // --- register ---

    @Test
    void registerReturnsAuthResponseOnSuccess() {
        RegisterRequest request = new RegisterRequest("Alice", "9876543210", "secret123", "USER");
        given(userAccountRepository.existsByPhone("9876543210")).willReturn(false);
        given(passwordEncoder.encode("secret123")).willReturn("hashed-password");
        given(userAccountRepository.save(any())).willReturn(savedUser);
        given(jwtService.issueToken(userId, "USER")).willReturn("jwt-token");
        given(jwtService.expirySeconds()).willReturn(3600L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.userId()).isEqualTo(userId.toString());
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void registerThrowsWhenPhoneAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("Alice", "9876543210", "secret123", "USER");
        given(userAccountRepository.existsByPhone("9876543210")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone already registered");

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenRoleIsInvalid() {
        RegisterRequest request = new RegisterRequest("Alice", "9876543210", "secret123", "UNKNOWN_ROLE");
        given(userAccountRepository.existsByPhone("9876543210")).willReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerWorksForDriverRole() {
        UserAccount driverAccount = new UserAccount();
        driverAccount.setName("Bob");
        driverAccount.setPhone("9123456780");
        driverAccount.setPasswordHash("hash");
        driverAccount.setRole(Role.DRIVER);
        UUID driverId = UUID.randomUUID();
        ReflectionTestUtils.setField(driverAccount, "id", driverId);

        RegisterRequest request = new RegisterRequest("Bob", "9123456780", "pass123", "DRIVER");
        given(userAccountRepository.existsByPhone("9123456780")).willReturn(false);
        given(passwordEncoder.encode("pass123")).willReturn("hash");
        given(userAccountRepository.save(any())).willReturn(driverAccount);
        given(jwtService.issueToken(driverId, "DRIVER")).willReturn("driver-token");
        given(jwtService.expirySeconds()).willReturn(3600L);

        AuthResponse response = authService.register(request);

        assertThat(response.role()).isEqualTo("DRIVER");
        assertThat(response.userId()).isEqualTo(driverId.toString());
    }

    // --- login ---

    @Test
    void loginReturnsAuthResponseOnSuccess() {
        LoginRequest request = new LoginRequest("9876543210", "secret123");
        given(userAccountRepository.findByPhone("9876543210")).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("secret123", "hashed-password")).willReturn(true);
        given(jwtService.issueToken(userId, "USER")).willReturn("login-token");
        given(jwtService.expirySeconds()).willReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("login-token");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void loginThrowsWhenPhoneNotFound() {
        LoginRequest request = new LoginRequest("0000000000", "secret123");
        given(userAccountRepository.findByPhone("0000000000")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("9876543210", "wrong-password");
        given(userAccountRepository.findByPhone("9876543210")).willReturn(Optional.of(savedUser));
        given(passwordEncoder.matches("wrong-password", "hashed-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void meReturnsCurrentUserProfile() {
        given(userAccountRepository.findById(userId)).willReturn(Optional.of(savedUser));

        UserProfileResponse response = authService.me(userId);

        assertThat(response.userId()).isEqualTo(userId.toString());
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.phone()).isEqualTo("9876543210");
        assertThat(response.role()).isEqualTo("USER");
    }
}
