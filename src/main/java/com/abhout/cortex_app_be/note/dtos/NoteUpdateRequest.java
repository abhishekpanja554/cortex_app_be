package com.abhout.cortex_app_be.note.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoteUpdateRequest(
        @NotBlank
        String title,
        @NotNull
        String body
) {
}
