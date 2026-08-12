package com.nakivo.job_processing.job.service;

import com.nakivo.job_processing.job.enumeric.JobProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessingListener {

    private final JobProcessService jobProcessService;

    private final ApplicationEventPublisher eventPublisher;

    @Async("jobTaskExecutor")
    @EventListener
    public void handle(Long jobId) {
        log.info("Processing job {}", jobId);

        try {
            JobProcessResult result = jobProcessService.handleProcessingJobById(jobId);

            if (result == JobProcessResult.RETRY) {
                eventPublisher.publishEvent(jobId);
            }
        } catch (Exception e) {
            log.error("[JobProcessingListener] Failed to process job {}", jobId, e);
        }
    }
}
