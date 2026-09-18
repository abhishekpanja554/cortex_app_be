package com.abhout.cortex_app_be.jobs.services;

import com.abhout.cortex_app_be.config.JobProperties;
import com.abhout.cortex_app_be.jobs.entities.EmbeddingJob;
import com.abhout.cortex_app_be.jobs.entities.JobStatus;
import com.abhout.cortex_app_be.jobs.repositories.EmbeddingJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobTimeoutSweeper {
    private final EmbeddingJobRepository embeddingJobRepository;
    private final JobProperties jobProperties;

    public JobTimeoutSweeper(EmbeddingJobRepository embeddingJobRepository, JobProperties jobProperties) {
        this.embeddingJobRepository = embeddingJobRepository;
        this.jobProperties = jobProperties;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void sweep(){
        Instant threshold = Instant.now().minus(jobProperties.getTimeoutThreshold());
        List<EmbeddingJob> jobs = embeddingJobRepository.findByStatusAndCreatedAtBefore(JobStatus.PENDING, threshold);
        jobs.forEach(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Timed out waiting for cortex-ai callback");
            embeddingJobRepository.save(job);
        });
    }
}
