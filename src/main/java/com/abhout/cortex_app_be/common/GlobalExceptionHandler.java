package com.abhout.cortex_app_be.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request){
        if (ex instanceof ErrorResponse errorResponse) {
            logHandled(errorResponse.getStatusCode(), "REQUEST_ERROR", ex, request);
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(ApiResponse.error(
                            "REQUEST_ERROR",
                            errorResponse.getBody().getDetail(),
                            MDC.get("requestId"))
                    );
        }
        ErrorDetails error = new ErrorDetails(
                "INTERNAL_ERROR",
                "Something went wrong. Please try after some time",
                MDC.get("requestId")
        );
        logHandled(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex, request);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error)
        );
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        logHandled(ex.getStatus(), ex.getErrorCode(), ex, request);
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), MDC.get("requestId"))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> e.getField() + " " + e.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        logHandled(HttpStatus.BAD_REQUEST, "METHOD_ARG_NOT_VALID", message, ex, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.error("METHOD_ARG_NOT_VALID",message, MDC.get("requestId"))
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));
        logHandled(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", message, ex, request);
        return ResponseEntity.badRequest().body(ApiResponse.error("CONSTRAINT_VIOLATION", message, MDC.get("requestId"))
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = ex.getName() + " has an invalid value: " + ex.getValue();
        logHandled(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, ex, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.error("INVALID_PARAMETER", message, MDC.get("requestId"))
        );
    }

    // Missing/unparseable body or a JSON value of the wrong type. Jackson's own message names
    // internal classes, so it's logged rather than returned.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", ex, request);
        return ResponseEntity.badRequest().body(
                ApiResponse.error("MALFORMED_REQUEST_BODY",
                        "Request body is missing or is not valid JSON for this endpoint",
                        MDC.get("requestId"))
        );
    }

    private void logHandled(HttpStatusCode status, String errorCode, Exception ex, HttpServletRequest request) {
        logHandled(status, errorCode, ex.getMessage(), ex, request);
    }

    private void logHandled(HttpStatusCode status, String errorCode, String message, Exception ex, HttpServletRequest request) {
        if (status.is5xxServerError()) {
            logger.error("{} {} failed with {} [{}]: {}",
                    request.getMethod(), request.getRequestURI(), status.value(), errorCode, message, ex);
        } else {
            logger.warn("{} {} rejected with {} [{}] {}: {} (thrown at {})",
                    request.getMethod(), request.getRequestURI(), status.value(), errorCode,
                    ex.getClass().getName(), message, throwSite(ex));
        }
    }

    // Top frame of the root cause.
    private static String throwSite(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        StackTraceElement[] trace = root.getStackTrace();
        return trace.length > 0 ? trace[0].toString() : "unknown";
    }
}
