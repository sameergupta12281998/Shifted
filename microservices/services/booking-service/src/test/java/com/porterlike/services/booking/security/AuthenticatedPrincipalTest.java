package com.porterlike.services.booking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTest {

    @Test
    void ofCreatesValidPrincipalWithUpperCasedRole() {
        UUID userId = UUID.randomUUID();

        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "user");

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo("USER");
    }

    @Test
    void ofThrowsSecurityExceptionWhenUserIdIsBlank() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of("  ", "USER"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void ofThrowsSecurityExceptionWhenUserIdIsNull() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of(null, "USER"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void ofThrowsSecurityExceptionWhenRoleIsBlank() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> AuthenticatedPrincipal.of(userId.toString(), ""))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void isAdminReturnsTrueForAdminRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "ADMIN");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void isAdminReturnsFalseForNonAdminRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "USER");
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void isUserReturnsTrueForUserRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "USER");
        assertThat(principal.isUser()).isTrue();
    }

    @Test
    void ofThrowsWhenUserIdIsNotValidUuid() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of("not-a-uuid", "USER"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
