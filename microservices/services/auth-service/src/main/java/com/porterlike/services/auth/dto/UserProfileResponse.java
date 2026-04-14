package com.porterlike.services.auth.dto;

public record UserProfileResponse(
        String userId,
        String name,
        String phone,
        String role
) {
}