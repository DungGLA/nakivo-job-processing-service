package com.nakivo.job_processing.job.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nakivo.job_processing.common.exception.JobNotFoundException;
import com.nakivo.job_processing.common.response.PageResponse;
import com.nakivo.job_processing.job.converter.JobConverter;
import com.nakivo.job_processing.job.dto.CreatedJobResponse;
import com.nakivo.job_processing.job.dto.JobDetailResponse;
import com.nakivo.job_processing.job.dto.JobRequest;
import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock
    JobRepository jobRepository;

    @Mock
    JobConverter jobConverter;

    JobService jobService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = Job.builder()
                .id(1L)
                .type("EMAIL")
                .status(JobStatus.PENDING)
                .payload("""
                        {"recipient":"test@example.com",
                        "subject":"Test Email"}
                        """)
                .createdAt(Instant.now())
                .build();
        jobService = new JobService(jobRepository, jobConverter);
    }

    @Test
    void givenJobRequest_whenCreateJob_thenCreateJobSuccess() {
        JsonNode payload = mock(JsonNode.class);
        when(payload.toString()).thenReturn("""
                {"recipient":"test@example.com"}
                """);

        JobRequest request = new JobRequest();
        request.setType("EMAIL");
        request.setPayload(payload);

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> {
                    Job savedJob = invocation.getArgument(0);
                    savedJob.setId(1L);
                    return savedJob;
                });

        CreatedJobResponse response = jobService.createJob(request);
        assertNotNull(response);
        assertEquals(1L, response.getJobId());
    }

    @Test
    void givenValidJobId_whenGetJobById_thenReturnJob() {
        JobDetailResponse expectedResponse = JobDetailResponse.builder()
                .id(1L)
                .type("EMAIL")
                .status(JobStatus.PENDING)
                .build();

        when(jobRepository.findById(1L))
                .thenReturn(Optional.of(job));

        when(jobConverter.toResource(job))
                .thenReturn(expectedResponse);

        JobDetailResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("EMAIL", response.getType());
        assertEquals(JobStatus.PENDING, response.getStatus());
        verify(jobRepository).findById(1L);
        verify(jobConverter).toResource(job);

    }

    @Test
    void givenInvalidJobId_whenGetJobById_thenThrowNotFound() {
        when(jobRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobById(999L));
        verify(jobRepository).findById(999L);
        verifyNoInteractions(jobConverter);
    }

    @Test
    void givenStatusPendingRequest_whenGetJobs_thenReturnListResult() {
        String statusRequest = "PENDING";
        Pageable pageableRequest = PageRequest.of(0, 10);

        JobDetailResponse response = JobDetailResponse.builder()
                .id(1L)
                .type("EMAIL")
                .status(JobStatus.PENDING)
                .build();

        Page<Job> jobPage = new PageImpl<>(List.of(job), pageableRequest, 1);

        when(jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING, pageableRequest)).thenReturn(jobPage);

        when(jobConverter.toResource(job)).thenReturn(response);

        PageResponse<JobDetailResponse> result = jobService.getJobs(statusRequest, pageableRequest);

        assertNotNull(result);
        verify(jobRepository).findByStatusOrderByCreatedAtAsc(JobStatus.PENDING, pageableRequest);
        verify(jobConverter).toResource(job);
    }
}