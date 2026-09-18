package com.abhout.cortex_app_be.jobs.repositories;

import com.abhout.cortex_app_be.jobs.entities.EmbeddingJob;
import com.abhout.cortex_app_be.jobs.entities.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, UUID> {
    List<EmbeddingJob> findByStatusAndCreatedAtBefore(JobStatus status, Instant threshold);
}
