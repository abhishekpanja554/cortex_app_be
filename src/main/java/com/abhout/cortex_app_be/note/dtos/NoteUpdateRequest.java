package com.abhout.cortex_app_be.note.dtos;

import jakarta.validation.constraints.NotNull;

public record NoteUpdateRequest(
        @NotNull
        String title,
        @NotNull
        String body
) {
}
