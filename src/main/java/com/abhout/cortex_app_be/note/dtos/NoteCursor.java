package com.abhout.cortex_app_be.note.dtos;

import com.abhout.cortex_app_be.note.exceptions.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record NoteCursor(
        Instant updatedAt, UUID id
) {
    private static final String SEPARATOR = ":";

    public String encode() {
        String raw = updatedAt.toEpochMilli() + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static NoteCursor decode(String raw) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(raw),
                    StandardCharsets.UTF_8
            );

            int separatorIndex = decoded.indexOf(SEPARATOR);
            if (separatorIndex < 0) {
                throw new InvalidCursorException("Cursor missing separator");
            }

            String epochMillisPart = decoded.substring(0, separatorIndex);
            String idPart = decoded.substring(separatorIndex + 1);

            Instant updatedAt = Instant.ofEpochMilli(Long.parseLong(epochMillisPart));
            UUID id = UUID.fromString(idPart);

            return new NoteCursor(updatedAt, id);
        } catch (InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCursorException("Malformed cursor");
        }
    }
}
