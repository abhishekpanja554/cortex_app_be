package com.abhout.cortex_app_be.jobs.dtos;

import com.abhout.cortex_app_be.jobs.entities.JobType;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingJobMessage(
        UUID jobId,
        UUID noteId,
        UUID ownerId,
        JobType jobType,
        String title,
        String body,
        Instant publishedAt)
{
}
