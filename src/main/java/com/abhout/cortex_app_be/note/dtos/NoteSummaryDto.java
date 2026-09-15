package com.abhout.cortex_app_be.note.dtos;

import java.time.Instant;
import java.util.UUID;

public record NoteSummaryDto(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
