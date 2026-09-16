package com.abhout.cortex_app_be.sync.dtos;

import java.util.List;

public record PushResponse(
        List<PushResultItem> results
) {
}
