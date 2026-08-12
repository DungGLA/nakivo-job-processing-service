package com.nakivo.job_processing.job.controller;

import com.nakivo.job_processing.job.dto.CreatedJobResponse;
import com.nakivo.job_processing.job.dto.JobDetailResponse;
import com.nakivo.job_processing.job.dto.JobRequest;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.service.JobProcessService;
import com.nakivo.job_processing.job.service.JobService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@AllArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobProcessService jobProcessService;

    @PostMapping
    public CreatedJobResponse create(@RequestBody JobRequest request) {
        return jobService.createJob(request);
    }

    @GetMapping("/{id}")
    public JobDetailResponse getJobById(@PathVariable Long id) {
        return jobService.getJobById(id);
    }

    @GetMapping
    public List<JobDetailResponse> getJobs(@PageableDefault(page = 0, size = 10) Pageable pageable, @RequestParam JobStatus status) {
        return jobService.getJobs(status, pageable);
    }

    @PostMapping("/process")
    public void processJob() {
        jobProcessService.processJob();
    }
}
