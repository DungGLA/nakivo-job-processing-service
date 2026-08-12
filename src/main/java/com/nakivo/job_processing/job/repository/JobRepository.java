package com.nakivo.job_processing.job.repository;

import com.nakivo.job_processing.job.entity.Job;
import com.nakivo.job_processing.job.enumeric.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByStatusOrderByCreatedAtAsc(JobStatus status, Pageable pageable);

//    @Modifying
//    @Query("UPDATE Job j SET j.status = 'PROCESSING', " +
//            "j.updatedAt = :updatedAt " +
//            "WHERE j.status = 'PENDING'")
//    int updatePendingStatus(Instant updatedAt);


    @Query(value = """
        SELECT *
        FROM job
        WHERE status = :status
        ORDER BY id
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Job> getJobsByStatusForUpdate(
            String status,
            int batchSize
    );

    @Modifying
    @Query(value = """
        UPDATE Job j
        SET j.status = :status,
            j.updatedAt = :updatedAt
        WHERE j.id IN (:ids)
    """)
    int updateBatchJobStatusById(List<Long> ids, JobStatus status, Instant updatedAt);
}
