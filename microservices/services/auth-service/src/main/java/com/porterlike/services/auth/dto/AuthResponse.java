package com.porterlike.services.auth.dto;

public record AuthResponse(
        String token,
        long expiresIn,
        String role,
        String userId
) {
}
