package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AttachmentIdTakenException extends BaseException {
    public AttachmentIdTakenException(UUID attachmentId) {
        super("ATTACHMENT_ID_TAKEN", "Attachment id already in use: " + attachmentId, HttpStatus.CONFLICT);
    }
}
