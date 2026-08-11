package com.nakivo.job_processing.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nakivo.job_processing.job.dto.CreatedJobResponse;
import com.nakivo.job_processing.job.dto.JobDetailResponse;
import com.nakivo.job_processing.job.dto.JobRequest;
import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    private final ObjectMapper objectMapper;

    public CreatedJobResponse createJob(JobRequest request) {
        Job job = Job.builder()
                .type(request.getType())
                .createdAt(Instant.now())
                .payload(request.getPayload().toString())
                .status(JobStatus.PENDING)
                .build();
        jobRepository.save(job);

        CreatedJobResponse response = new CreatedJobResponse();
        response.setId(job.getId());
        return response;
    }

    public JobDetailResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        JobDetailResponse response = new JobDetailResponse();
        response.setId(job.getId());
        response.setType(job.getType());
        response.setStatus(job.getStatus());
        response.setPayload(getPayload(job.getPayload()));
        response.setRetryCount(job.getRetryCount());
        response.setErrorMessage(job.getErrorMessage());

        return response;
    }

    public List<JobDetailResponse> getJobs(JobStatus status, Pageable pageable) {
        List<Job> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(status, pageable).getContent();
        return jobs.stream().map(job -> {
            JobDetailResponse response = new JobDetailResponse();
            response.setId(job.getId());
            response.setType(job.getType());
            response.setStatus(job.getStatus());
            response.setPayload(getPayload(job.getPayload()));
            response.setRetryCount(job.getRetryCount());
            response.setErrorMessage(job.getErrorMessage());
            return response;
        }).toList();
    }

    private JsonNode getPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid job payload", e);
        }
    }
}
