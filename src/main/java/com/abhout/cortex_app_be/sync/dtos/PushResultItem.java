package com.abhout.cortex_app_be.sync.dtos;

import java.util.UUID;

public record PushResultItem(
        UUID id,
        PushOutcome titleOutcome,
        PushOutcome bodyOutcome,
        UUID conflictNoteId
) {
}
