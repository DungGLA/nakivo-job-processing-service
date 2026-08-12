package com.nakivo.job_processing.common.exception;

import com.nakivo.job_processing.common.enumeric.ErrorCode;

public class InvalidJobStatusException extends JobException {
    private static final String DEFAULT_MESSAGE = "Job status is invalid.";

    public InvalidJobStatusException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidJobStatusException(String message) {
        super(ErrorCode.INVALID_JOB_STATUS, message);
    }
}
