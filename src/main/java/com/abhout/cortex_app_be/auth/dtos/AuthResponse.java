package com.abhout.cortex_app_be.auth.dtos;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
