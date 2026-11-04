package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AttachmentNotFoundException extends BaseException {
    public AttachmentNotFoundException(UUID attachmentId) {
        super("ATTACHMENT_NOT_FOUND", "Attachment not found: " + attachmentId, HttpStatus.NOT_FOUND);
    }
}
