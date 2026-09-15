package com.abhout.cortex_app_be.auth.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BaseException {

    public InvalidRefreshTokenException(String message) {
        super("INVALID_REFRESH_TOKEN", message, HttpStatus.UNAUTHORIZED);
    }
}
