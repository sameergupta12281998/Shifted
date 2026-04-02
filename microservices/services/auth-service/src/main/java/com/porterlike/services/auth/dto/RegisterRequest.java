package com.porterlike.services.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$") String phone,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotBlank String role
) {
}
