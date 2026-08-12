package com.nakivo.job_processing.common.enumeric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    JOB_NOT_FOUND("JOB_NOT_FOUND", HttpStatus.NOT_FOUND),

    JOB_ALREADY_PROCESSING("JOB_ALREADY_PROCESSING", HttpStatus.CONFLICT),

    INVALID_JOB_STATUS("INVALID_JOB_STATUS", HttpStatus.BAD_REQUEST),

    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;
}
