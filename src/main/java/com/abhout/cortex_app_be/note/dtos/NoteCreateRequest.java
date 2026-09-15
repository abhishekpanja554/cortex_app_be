package com.abhout.cortex_app_be.note.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoteCreateRequest(
        @NotBlank
        String title,
        @NotNull
        String body
) {
}
