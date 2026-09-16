package com.abhout.cortex_app_be.sync.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PushRequest(
        @NotEmpty @Valid List<NotePushItem> notes
) {
}
