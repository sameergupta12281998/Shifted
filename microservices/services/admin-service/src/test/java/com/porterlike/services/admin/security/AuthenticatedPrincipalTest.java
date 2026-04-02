package com.porterlike.services.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTest {

    @Test
    void ofCreatesValidPrincipalWithUpperCasedRole() {
        UUID userId = UUID.randomUUID();

        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "admin");

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo("ADMIN");
    }

    @Test
    void ofThrowsSecurityExceptionWhenUserIdIsBlank() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of("  ", "ADMIN"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void ofThrowsSecurityExceptionWhenUserIdIsNull() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of(null, "ADMIN"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void isAdminReturnsTrueForAdminRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "ADMIN");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void isAdminReturnsFalseForUserRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "USER");
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void ofThrowsWhenUserIdIsNotValidUuid() {
        assertThatThrownBy(() -> AuthenticatedPrincipal.of("not-a-uuid", "ADMIN"))
                .isInstanceOf(Exception.class);
    }
}
