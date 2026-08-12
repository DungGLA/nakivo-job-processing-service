package com.nakivo.job_processing.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessingListener {

    private final JobProcessService jobProcessService;

    @Async("jobTaskExecutor")
    @EventListener
    public void handle(Long jobId) {
        log.info("Processing job {}", jobId);

        try {
            jobProcessService.handleProcessingJobById(jobId);
        } catch (Exception e) {
            log.error("[JobProcessingListener] Failed to process job {}", jobId, e);
        }
    }
}
