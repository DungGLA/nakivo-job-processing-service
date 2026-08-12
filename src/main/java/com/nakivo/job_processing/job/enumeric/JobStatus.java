package com.nakivo.job_processing.job.enumeric;

import com.nakivo.job_processing.common.exception.InvalidJobStatusException;
import io.micrometer.common.util.StringUtils;

import java.util.Arrays;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public static JobStatus from(String value) {
        if (StringUtils.isBlank(value)) {
            throw new InvalidJobStatusException("Job status is empty");
        }

        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new InvalidJobStatusException("Invalid job status: " + value));
    }
}
