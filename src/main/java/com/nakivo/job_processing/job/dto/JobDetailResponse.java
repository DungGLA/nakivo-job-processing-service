package com.nakivo.job_processing.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobDetailResponse {
    private Long id;
    private String type;
    private JobStatus status;
    private JsonNode payload;
    private String errorMessage;
    private int retryCount;
}
