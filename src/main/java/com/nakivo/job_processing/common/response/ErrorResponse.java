package com.nakivo.job_processing.common.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private String code;

    private String message;

    private Instant timestamp;

    private String path;
}
