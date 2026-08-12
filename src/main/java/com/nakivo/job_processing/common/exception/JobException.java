package com.nakivo.job_processing.common.exception;

import com.nakivo.job_processing.common.enumeric.ErrorCode;
import lombok.Getter;

@Getter
public class JobException extends RuntimeException {
    private final ErrorCode errorCode;

    public JobException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
