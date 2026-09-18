package com.abhout.cortex_app_be.jobs.entities;

import com.abhout.cortex_app_be.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "embedding_jobs")
public class EmbeddingJob extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "note_id", nullable = false)
    private UUID noteId;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Column(name = "job_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobType jobType;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    public EmbeddingJob(UUID noteId, UUID ownerId, JobType type, JobStatus status) {
        this.noteId = noteId;
        this.ownerId = ownerId;
        this.jobType = type;
        this.status = status;
    }
}
