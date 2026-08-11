package com.nakivo.job_processing.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class JobRequest {
    private String type;
    private JsonNode payload;
}
