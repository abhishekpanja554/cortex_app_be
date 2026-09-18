package com.abhout.cortex_app_be.jobs.exceptions;

import com.abhout.cortex_app_be.common.BaseException;
import org.springframework.http.HttpStatus;

public class JobAlreadyTerminalException  extends BaseException {
    public JobAlreadyTerminalException(String message) {
        super("JOB_ALREADY_TERMINAL", message, HttpStatus.CONFLICT);
    }
}
