package com.nakivo.job_processing.common.exception;

import com.nakivo.job_processing.common.enumeric.ErrorCode;
import com.nakivo.job_processing.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return errors;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.builder()
                        .code("INVALID_REQUEST_PARAMETER")
                        .message(message)
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(JobException.class)
    public ResponseEntity<ErrorResponse> handleJobException(JobException ex, HttpServletRequest request) {
        log.warn(
                "JobException: code={}, message={}, path={}",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled exception at path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
                        .message("Unexpected error occurred")
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }
}
