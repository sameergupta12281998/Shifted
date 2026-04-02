package com.porterlike.services.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // Must be at least 32 bytes for HMAC-SHA256
    private static final String TEST_SECRET = "test-secret-key-minimum-32bytes!";
    private static final long EXPIRY_SECONDS = 3600L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRY_SECONDS);
    }

    @Test
    void issueTokenReturnsNonBlankString() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId, "USER");

        assertThat(token).isNotBlank();
    }

    @Test
    void issueTokenContainsSubjectAndRoleClaim() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId, "DRIVER");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
    }

    @Test
    void expirySecondsReturnsConfiguredValue() {
        assertThat(jwtService.expirySeconds()).isEqualTo(EXPIRY_SECONDS);
    }

    @Test
    void issueTokenWithAdminRoleEmbedsClaim() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.issueToken(userId, "ADMIN");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }
}
