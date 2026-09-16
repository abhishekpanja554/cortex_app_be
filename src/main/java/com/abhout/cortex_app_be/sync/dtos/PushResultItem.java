package com.abhout.cortex_app_be.sync.dtos;

import java.util.UUID;

public record PushResultItem(
        UUID id,
        PushOutcome outcome
) {
}
