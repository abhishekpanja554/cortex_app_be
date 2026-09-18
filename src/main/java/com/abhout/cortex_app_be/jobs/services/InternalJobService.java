package com.abhout.cortex_app_be.jobs.services;

import com.abhout.cortex_app_be.jobs.entities.EmbeddingJob;
import com.abhout.cortex_app_be.jobs.entities.JobStatus;
import com.abhout.cortex_app_be.jobs.exceptions.EmbeddingJobNotFoundException;
import com.abhout.cortex_app_be.jobs.exceptions.JobAlreadyTerminalException;
import com.abhout.cortex_app_be.jobs.repositories.EmbeddingJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InternalJobService {

    private final EmbeddingJobRepository embeddingJobRepository;

    public InternalJobService(EmbeddingJobRepository embeddingJobRepository) {
        this.embeddingJobRepository = embeddingJobRepository;
    }

    @Transactional
    public void complete(UUID jobId) {
        EmbeddingJob job = embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new EmbeddingJobNotFoundException("Job not found: " + jobId));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new JobAlreadyTerminalException("Job is already " + job.getStatus());
        }
        job.setStatus(JobStatus.COMPLETED);
        embeddingJobRepository.save(job);
    }

    @Transactional
    public void fail(UUID jobId, String errorMessage) {
        EmbeddingJob job = embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new EmbeddingJobNotFoundException("Job not found: " + jobId));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new JobAlreadyTerminalException("Job is already completed or failed");
        }
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        embeddingJobRepository.save(job);
    }
}
