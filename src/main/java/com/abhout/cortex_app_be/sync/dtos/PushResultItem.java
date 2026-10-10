package com.abhout.cortex_app_be.sync.dtos;

import java.time.Instant;
import java.util.UUID;

// titleOutcome/bodyOutcome are null for a field the device didn't send.
// titleUpdatedAt/bodyUpdatedAt are the server's stamps for that field, returned only when
// the device's copy now matches the server's (ACCEPTED or REJECTED_STALE), so the device
// can use them as its next base without a pull. Null otherwise: on CONFLICT the device
// doesn't have the server's content, so adopting the stamp would let it overwrite unseen data.
// deleted = true means the note is gone on the server; the device should drop it.
public record PushResultItem(
        UUID id,
        PushOutcome titleOutcome,
        PushOutcome bodyOutcome,
        Instant titleUpdatedAt,
        Instant bodyUpdatedAt,
        UUID conflictNoteId,
        boolean deleted
) {
}
