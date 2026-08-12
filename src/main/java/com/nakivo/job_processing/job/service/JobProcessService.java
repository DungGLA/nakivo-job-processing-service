package com.nakivo.job_processing.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class JobProcessService {

    private final JobService jobService;

    private final JobRepository jobRepository;

    private final ApplicationEventPublisher eventPublisher;

    public void processJob() {
        boolean hasProcessingJobs;
        do {
            List<Long> jobIds = jobService.claimJobs();
            hasProcessingJobs = !jobIds.isEmpty();

            if (hasProcessingJobs) {
                jobIds.forEach(eventPublisher::publishEvent);

            }
        } while (hasProcessingJobs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProcessingJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        job.setUpdatedAt(Instant.now());

        if (isFailedJob(job)) {
            handleFailedJob(job);
            return;
        }

        job.setStatus(JobStatus.COMPLETED);
    }

    private void handleFailedJob(Job job) {
        if (Objects.isNull(job)) return;

        if (job.getRetryCount() < 3) {
            job.setRetryCount(job.getRetryCount() + 1);
            eventPublisher.publishEvent(job.getId());
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Job failed after 3 retries.");
        }
    }

    private boolean isFailedJob(Job job) {
        JsonNode payload = jobService.getPayload(job.getPayload());
        return payload.path("fail").asBoolean(false);
    }

}
