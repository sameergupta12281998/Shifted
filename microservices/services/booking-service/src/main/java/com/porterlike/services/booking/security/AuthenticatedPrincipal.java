package com.porterlike.services.booking.security;

import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String role
) {

    public static AuthenticatedPrincipal of(String userId, String role) {
        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            throw new SecurityException("Missing or empty authentication headers");
        }
        return new AuthenticatedPrincipal(UUID.fromString(userId), role.toUpperCase());
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isUser() {
        return "USER".equals(role);
    }
}