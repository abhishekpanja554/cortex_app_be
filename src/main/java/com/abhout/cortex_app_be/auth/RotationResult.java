package com.abhout.cortex_app_be.auth;

import com.abhout.cortex_app_be.user.entities.User;

public record RotationResult(
        String rawToken,
        User user
) {
}
