package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class UnsupportedAttachmentTypeException extends BaseException {
    public UnsupportedAttachmentTypeException() {
        super("UNSUPPORTED_ATTACHMENT_TYPE", "Only JPEG, PNG, WebP and HEIC images are accepted", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
