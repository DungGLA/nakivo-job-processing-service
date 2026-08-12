package com.nakivo.job_processing.job.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class JobProcessService {

    private final JobService jobService;

    @Transactional
    public void processJob() {
        boolean getPendingJob = true;
        do {
            int numberJobs = jobService.claimJobs();
            if (numberJobs == 0) {
                getPendingJob = false;
            }
        } while (getPendingJob);
    }
}
