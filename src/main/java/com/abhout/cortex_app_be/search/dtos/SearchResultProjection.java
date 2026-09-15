package com.abhout.cortex_app_be.search.dtos;

import java.time.Instant;
import java.util.UUID;

public interface SearchResultProjection {
    UUID getId();
    String getTitle();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Double getRank();
}
