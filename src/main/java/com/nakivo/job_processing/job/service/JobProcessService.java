package com.nakivo.job_processing.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job_processing.common.exception.JobNotFoundException;
import com.nakivo.job_processing.common.helper.JsonNodeConverter;
import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobProcessResult;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class JobProcessService {

    private final JobService jobService;

    private final JobRepository jobRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final JsonNodeConverter jsonNodeConverter;

    private static final int MAX_RETRY_COUNT = 3;

    private static final int BATCH_SIZE = 5;

    public void processJob() {
        boolean hasProcessingJobs;
        do {
            List<Long> jobIds = jobService.processJobByBatch(BATCH_SIZE);
            hasProcessingJobs = !jobIds.isEmpty();

            if (hasProcessingJobs) {
                jobIds.forEach(eventPublisher::publishEvent);

            }
        } while (hasProcessingJobs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobProcessResult handleProcessingJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Can not find job with the given ID: " + jobId));
        job.setUpdatedAt(Instant.now());

        if (!isFailedJob(job)) {
            job.setStatus(JobStatus.COMPLETED);
            return JobProcessResult.COMPLETED;
        }

        int currentRetryCount = job.getRetryCount();

        if (currentRetryCount >= MAX_RETRY_COUNT) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Job failed after " + currentRetryCount + " retries.");

            log.info("Failed job {} after {} retries", job.getId(), currentRetryCount);
            return JobProcessResult.FAILED;
        }

        job.setRetryCount(currentRetryCount + 1);
        log.info("Failed job {} at attempt {}, need to retry", job.getId(), currentRetryCount);

        return JobProcessResult.RETRY;
    }

    private boolean isFailedJob(Job job) {
        JsonNode payload = jsonNodeConverter.convertStringToJsonNode(job.getPayload());
        return payload.path("fail").asBoolean(false);
    }

}
