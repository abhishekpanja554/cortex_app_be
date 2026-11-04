package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class AttachmentTooLargeException extends BaseException {
    public AttachmentTooLargeException(long maxBytes) {
        super("ATTACHMENT_TOO_LARGE", "Attachment exceeds the " + maxBytes + " byte limit", HttpStatus.CONTENT_TOO_LARGE);
    }
}
