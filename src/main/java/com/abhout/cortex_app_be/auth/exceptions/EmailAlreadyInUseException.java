package com.abhout.cortex_app_be.auth.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends BaseException {
    public EmailAlreadyInUseException(String message) {
        super("EMAIL_ALREADY_IN_USE", message, HttpStatus.CONFLICT);
    }
}
