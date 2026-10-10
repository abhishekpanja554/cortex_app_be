package com.abhout.cortex_app_be.sync.dtos;

public enum PushOutcome {
    ACCEPTED,
    REJECTED_STALE,
    CONFLICT,
    // The note was deleted on the server; delete wins over any edit.
    REJECTED_DELETED
}
