package com.nakivo.job_processing.job.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatedJobResponse {
    private Long jobId;
}
