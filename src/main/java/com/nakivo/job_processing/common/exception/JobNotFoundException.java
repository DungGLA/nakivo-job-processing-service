package com.nakivo.job_processing.common.exception;

import com.nakivo.job_processing.common.enumeric.ErrorCode;

public class JobNotFoundException extends JobException {

    private static final String DEFAULT_MESSAGE = "Can not find job with the given ID. Please check the job ID and try again";

    public JobNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public JobNotFoundException(String message) {
        super(ErrorCode.JOB_NOT_FOUND, message);
    }
}
