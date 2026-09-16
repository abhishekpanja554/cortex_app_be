package com.abhout.cortex_app_be.sync.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record NotePushItem(
        @NotNull UUID id,
        @NotBlank String title,
        @NotNull String body,
        @NotNull Instant clientUpdatedAt
) {
}
