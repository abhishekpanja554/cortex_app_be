package com.abhout.cortex_app_be.search.dtos;

import com.abhout.cortex_app_be.note.exceptions.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public record SearchCursor(
        double rank,
        UUID id
) {
    private static final String SEPARATOR = ":";

    public String encode() {
        String raw = rank + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static SearchCursor decode(String raw) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(raw),
                    StandardCharsets.UTF_8
            );

            int separatorIndex = decoded.indexOf(SEPARATOR);
            if (separatorIndex < 0) {
                throw new InvalidCursorException("Cursor missing separator");
            }

            String rankPart = decoded.substring(0, separatorIndex);
            String idPart = decoded.substring(separatorIndex + 1);

            double rank = Double.parseDouble(rankPart);
            UUID id = UUID.fromString(idPart);

            return new SearchCursor(rank, id);
        } catch (InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCursorException("Malformed cursor");
        }
    }
}
