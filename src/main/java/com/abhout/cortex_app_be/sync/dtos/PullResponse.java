package com.abhout.cortex_app_be.sync.dtos;

import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;

import java.time.Instant;
import java.util.List;

// notes includes tombstones (deletedAt != null).
// nextSince is the `since` to send on the next pull. It overlaps the previous window
// on purpose, so a note can arrive more than once; clients apply it idempotently.
public record PullResponse(
        List<NoteDetailDto> notes,
        Instant nextSince
) {
}
