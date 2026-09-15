package com.abhout.cortex_app_be.note.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidCursorException extends BaseException {
    public InvalidCursorException(String message) {
        super("INVALID_CURSOR", message, HttpStatus.BAD_REQUEST);
    }
}
