package com.nakivo.job_processing.job.service;


import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import com.nakivo.job_processing.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JobProcessConcurrencyIntegrationTest {

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final JobProcessService jobProcessService;

    JobProcessConcurrencyIntegrationTest(
            JobService jobService,
            JobRepository jobRepository,
            JobProcessService jobProcessService
    ) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.jobProcessService = jobProcessService;
    }


    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void givenTenPendingJob_whenProcessTwoConcurrentRequest_thenTwoFlowDoNotQueryDuplicateData() {

        // Given
        for (int i = 0; i < 10; i++) {
            jobRepository.save(
                    Job.builder()
                            .type("TEST")
                            .payload("{\"fail\":false}")
                            .status(JobStatus.PENDING)
                            .createdAt(Instant.now())
                            .build()
            );
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // When
            CompletableFuture<List<Long>> future1 =
                    CompletableFuture.supplyAsync(() -> jobService.processJobByBatch(5), executor);

            CompletableFuture<List<Long>> future2 =
                    CompletableFuture.supplyAsync(() -> jobService.processJobByBatch(5), executor);

            List<Long> batch1 = future1.join();
            List<Long> batch2 = future2.join();

            // Then
            assertThat(batch1).hasSize(5).doesNotContainAnyElementsOf(batch2);

            assertThat(batch2).hasSize(5);

            List<Long> allClaimedJobIds = Stream.concat(batch1.stream(), batch2.stream()).toList();

            assertThat(allClaimedJobIds).hasSize(10).doesNotHaveDuplicates();

            List<Job> jobs = jobRepository.findAll();

            assertThat(jobs).hasSize(10).allMatch(job -> job.getStatus() == JobStatus.PROCESSING);

        } finally {
            executor.shutdown();
        }
    }

    @Test
    void givenFiveFailedFiveSuccess_whenJobProcess_thenHandleCompletedAndRetryFailedJobs () {
        // Given
        for (int i = 1; i <= 10; i++) {
            boolean failed = i % 2 != 0;

            jobRepository.save(
                    Job.builder()
                            .type("TEST")
                            .payload(failed
                                    ? "{\"fail\":true}"
                                    : "{\"fail\":false}")
                            .status(JobStatus.PENDING)
                            .retryCount(0)
                            .createdAt(Instant.now())
                            .build()
            );
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // When
            CompletableFuture<Void> future1 =
                    CompletableFuture.runAsync(jobProcessService::processJob, executor);

            CompletableFuture<Void> future2 =
                    CompletableFuture.runAsync(jobProcessService::processJob, executor);

            CompletableFuture.allOf(future1, future2).join();
            Thread.sleep(1000);

            List<Job> jobs = jobRepository.findAll();
            assertThat(jobs).hasSize(10);

            List<Job> completedJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.COMPLETED).toList();
            List<Job> failedJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.FAILED).toList();

            assertThat(completedJobs)
                    .hasSize(5)
                    .allMatch(job ->
                            job.getStatus() == JobStatus.COMPLETED &&
                            job.getPayload().equals("{\"fail\":false}"));

            assertThat(failedJobs)
                    .hasSize(5)
                    .allMatch(job ->
                            job.getStatus() == JobStatus.FAILED &&
                            job.getRetryCount() == 3 &&
                            job.getErrorMessage() != null &&
                            job.getPayload().equals("{\"fail\":true}"));

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }
}