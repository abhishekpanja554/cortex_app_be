package com.abhout.cortex_app_be.jobs.dtos;

import jakarta.validation.constraints.NotBlank;

public record JobFailRequest(
        @NotBlank
        String errorMessage
) {
}
