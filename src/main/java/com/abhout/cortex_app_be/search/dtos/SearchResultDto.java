package com.abhout.cortex_app_be.search.dtos;

import java.time.Instant;
import java.util.UUID;

public record SearchResultDto(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        Double rank
) {
}
