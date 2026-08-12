package com.nakivo.job_processing.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job_processing.common.helper.NonEmptyJsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRequest {
    @NotBlank(message = "Type cannot be blank")
    private String type;

    @NonEmptyJsonNode(message = "Payload cannot be empty")
    private JsonNode payload;
}
