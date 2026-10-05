package com.abhout.cortex_app_be.search.dtos;

import java.util.UUID;

public record InternalSearchRequest(
        UUID ownerId,
        String query,
        int limit
) {
}
