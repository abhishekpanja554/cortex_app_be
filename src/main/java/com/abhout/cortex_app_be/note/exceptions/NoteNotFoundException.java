package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoteNotFoundException extends BaseException {
    public NoteNotFoundException(UUID noteId) {
        super( "NOTE_NOT_FOUND", "Note not found: " + noteId, HttpStatus.NOT_FOUND);
    }
}
