package com.abhout.cortex_app_be.sync.dtos;

import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;

import java.time.Instant;
import java.util.List;

public record PullResponse(
        List<NoteDetailDto> notes,
        Instant serverTime
) {
}
