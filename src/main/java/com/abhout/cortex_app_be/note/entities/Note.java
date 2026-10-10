package com.abhout.cortex_app_be.note.entities;

import com.abhout.cortex_app_be.common.Auditable;
import com.abhout.cortex_app_be.user.entities.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends Auditable implements Persistable<UUID> {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;
    @ManyToMany
    @JoinTable(
            name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"note_id", "tag_id"}
            )
    )
    private Set<Tag> tags = new HashSet<>();
    @OneToMany(mappedBy = "note", fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();
    @Column(name = "title_updated_at", nullable = false)
    private Instant titleUpdatedAt;
    @Column(name = "body_updated_at", nullable = false)
    private Instant bodyUpdatedAt;
    @Column(name = "conflict_of", nullable = true)
    private UUID conflictOf;
    @Column(name = "conflict_field", nullable = true)
    private String conflictField;
    @Column(name = "deleted_at", nullable = true)
    private Instant deletedAt;

    public Note(String title, String body, User owner) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.body = body;
        this.owner = owner;
        Instant now = versionStamp();
        this.titleUpdatedAt = now;
        this.bodyUpdatedAt = now;
    }

    // titleUpdatedAt/bodyUpdatedAt are version tokens compared with equals() during sync.
    // Postgres stores microseconds, so truncate here; otherwise the in-memory value we
    // return to the client differs from the stored one and every later push mismatches.
    public static Instant versionStamp() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
