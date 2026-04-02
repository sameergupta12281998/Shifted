package com.porterlike.services.driver.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTest {

    @Test
    void ofCreatesValidPrincipalWithUpperCasedRole() {
        UUID userId = UUID.randomUUID();

        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "driver");

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.role()).isEqualTo("DRIVER");
        assertThat(principal.isPresent()).isTrue();
    }

    @Test
    void optionalWithBothBlankReturnsAbsentPrincipal() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.optional("", "");

        assertThat(principal.isPresent()).isFalse();
        assertThat(principal.userId()).isNull();
    }

    @Test
    void optionalWithNullsReturnsAbsentPrincipal() {
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.optional(null, null);

        assertThat(principal.isPresent()).isFalse();
    }

    @Test
    void optionalWithValuesReturnsPresentPrincipal() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.optional(userId.toString(), "DRIVER");

        assertThat(principal.isPresent()).isTrue();
        assertThat(principal.userId()).isEqualTo(userId);
    }

    @Test
    void isAdminReturnsTrueForAdminRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "ADMIN");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void isDriverReturnsTrueForDriverRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "DRIVER");
        assertThat(principal.isDriver()).isTrue();
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void isDriverReturnsFalseForAdminRole() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), "ADMIN");
        assertThat(principal.isDriver()).isFalse();
    }

    @Test
    void ofNormalizesNullRoleToEmptyString() {
        UUID userId = UUID.randomUUID();
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.of(userId.toString(), null);

        assertThat(principal.role()).isEqualTo("");
        assertThat(principal.isAdmin()).isFalse();
        assertThat(principal.isDriver()).isFalse();
    }
}
