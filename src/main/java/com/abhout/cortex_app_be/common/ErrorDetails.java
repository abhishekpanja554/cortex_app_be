package com.abhout.cortex_app_be.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorDetails {
    private String code;
    private String message;
    private List<String> details;

    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
