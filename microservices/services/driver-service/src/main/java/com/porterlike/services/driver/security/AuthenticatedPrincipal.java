package com.porterlike.services.driver.security;

import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String role,
        boolean present
) {

    public static AuthenticatedPrincipal of(String userId, String role) {
        return new AuthenticatedPrincipal(UUID.fromString(userId), normalizeRole(role), true);
    }

    public static AuthenticatedPrincipal optional(String userId, String role) {
        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            return new AuthenticatedPrincipal(null, "", false);
        }
        return of(userId, role);
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.toUpperCase();
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isDriver() {
        return "DRIVER".equals(role);
    }
}