package com.nakivo.job_processing.job.converter;

import com.nakivo.job_processing.common.helper.JsonNodeConverter;
import com.nakivo.job_processing.job.dto.JobDetailResponse;
import com.nakivo.job_processing.job.entity.Job;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JobConverter {
    private final JsonNodeConverter jsonNodeConverter;

    public JobDetailResponse toResource(Job job) {
        return JobDetailResponse.builder()
                .id(job.getId())
                .type(job.getType())
                .status(job.getStatus())
                .payload(jsonNodeConverter.convertStringToJsonNode(job.getPayload()))
                .retryCount(job.getRetryCount())
                .errorMessage(job.getErrorMessage())
                .build();
    }

}
