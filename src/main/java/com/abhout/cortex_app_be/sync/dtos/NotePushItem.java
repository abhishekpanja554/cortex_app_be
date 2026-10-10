package com.abhout.cortex_app_be.sync.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

// title/body are optional: null means "this device didn't change it", and the server
// leaves that field alone. Each sent field carries the base stamp the device last saw
// for it. A new note is created from whatever is sent (missing fields become "").
// Empty strings are valid; a blank title must not reject the whole batch.
public record NotePushItem(
        @NotNull UUID id,
        String title,
        String body,
        Instant titleBaseUpdatedAt,
        Instant bodyBaseUpdatedAt,
        Boolean deleted
) {
    // Boolean, not boolean: Jackson 3 maps a missing field to null and rejects null for primitives.
    public NotePushItem {
        if (deleted == null) {
            deleted = false;
        }
    }
}
