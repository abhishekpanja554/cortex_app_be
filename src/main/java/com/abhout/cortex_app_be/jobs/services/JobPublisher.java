package com.abhout.cortex_app_be.jobs.services;

import com.abhout.cortex_app_be.config.JobProperties;
import com.abhout.cortex_app_be.jobs.dtos.EmbeddingJobMessage;
import com.abhout.cortex_app_be.jobs.entities.EmbeddingJob;
import com.abhout.cortex_app_be.jobs.entities.JobStatus;
import com.abhout.cortex_app_be.jobs.entities.JobType;
import com.abhout.cortex_app_be.jobs.repositories.EmbeddingJobRepository;
import com.abhout.cortex_app_be.note.entities.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class JobPublisher {
    private static final Logger logger = LoggerFactory.getLogger(JobPublisher.class);

    private final EmbeddingJobRepository embeddingJobRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JobProperties jobProperties;

    public JobPublisher(
            EmbeddingJobRepository embeddingJobRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            JobProperties jobProperties
    ){
        this.embeddingJobRepository = embeddingJobRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jobProperties = jobProperties;
    }

    public void publishEmbedJob(Note note) {
        publish(
                note.getId(),
                note.getOwner().getId(),
                JobType.EMBED,
                note.getTitle(),
                note.getBody()
        );
    }

    public void publishDeleteJob(UUID noteId, UUID ownerId)
    {
        publish(
                noteId,
                ownerId,
                JobType.DELETE_EMBEDDINGS,
                null,
                null
        );
    }

    private void publish(UUID noteId, UUID ownerId, JobType type, String title, String body)  {
        EmbeddingJob embeddingJob = new EmbeddingJob(
                noteId,
                ownerId,
                type,
                JobStatus.PENDING
        );
        embeddingJobRepository.save(embeddingJob);
        EmbeddingJobMessage message  = new EmbeddingJobMessage(
                embeddingJob.getId(),
                embeddingJob.getNoteId(),
                embeddingJob.getOwnerId(),
                embeddingJob.getJobType(),
                title,
                body,
                Instant.now()
        );

        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(jobProperties.getQueueKey(),json);
        } catch ( Exception e){
            logger.error("Failed to publish job to the queue", e);
            embeddingJob.setStatus(JobStatus.FAILED);
            embeddingJob.setErrorMessage("Failed to publish to queue: " + e.getMessage());
            embeddingJobRepository.save(embeddingJob);
        }

    }
}
