package com.nakivo.job_processing.job.service;

import com.nakivo.job_processing.common.exception.JobNotFoundException;
import com.nakivo.job_processing.common.helper.PageResponseMapper;
import com.nakivo.job_processing.common.response.PageResponse;
import com.nakivo.job_processing.job.converter.JobConverter;
import com.nakivo.job_processing.job.dto.CreatedJobResponse;
import com.nakivo.job_processing.job.dto.JobDetailResponse;
import com.nakivo.job_processing.job.dto.JobRequest;
import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;

    private final JobConverter jobConverter;

    public CreatedJobResponse createJob(JobRequest request) {
        Job job = Job.builder()
                .type(request.getType())
                .createdAt(Instant.now())
                .payload(request.getPayload().toString())
                .status(JobStatus.PENDING)
                .build();
        jobRepository.save(job);

        return CreatedJobResponse.builder().jobId(job.getId()).build();
    }

    public JobDetailResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Can not find job with the given ID: " + id));

        return jobConverter.toResource(job);
    }

    public PageResponse<JobDetailResponse> getJobs(String statusRequest, Pageable pageable) {
        JobStatus status = JobStatus.from(statusRequest);
        Page<JobDetailResponse> page = jobRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                .map(jobConverter::toResource);

        return PageResponseMapper.from(page);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> processJobByBatch(int batchSize) {
        List<Job> jobs = jobRepository.getJobsByStatusForUpdate(JobStatus.PENDING.name(), batchSize);

        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        if (!jobIds.isEmpty()) {

            jobRepository.updateBatchJobStatusById(
                    jobIds,
                    JobStatus.PROCESSING,
                    Instant.now());

            log.info("Claimed jobs:" + jobIds + " for processing.");
        }

        return jobIds;
    }
}
