package com.porterlike.services.admin.security;

import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String role
) {

    public static AuthenticatedPrincipal of(String userId, String role) {
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("Missing authentication headers");
        }
        return new AuthenticatedPrincipal(UUID.fromString(userId), role == null ? "" : role.toUpperCase());
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
