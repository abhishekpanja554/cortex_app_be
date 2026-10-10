package com.abhout.cortex_app_be.note.repositories;

import com.abhout.cortex_app_be.note.dtos.NoteSummaryDto;
import com.abhout.cortex_app_be.note.entities.Note;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    // Soft-deleted notes are invisible to normal reads. Sync is the exception:
    // push uses findByIdForUpdate (must see tombstones) and pull returns them so devices learn of deletes.
    Optional<Note> findByIdAndOwnerIdAndDeletedAtIsNull(UUID noteId, UUID ownerId);

    // SELECT ... FOR UPDATE, for every read-check-write on a note (push, PUT, DELETE).
    // Without the row lock, two concurrent writers both pass the base-stamp check against
    // the same old value and the later commit silently overwrites the earlier one.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Note n where n.id = :noteId")
    Optional<Note> findByIdForUpdate(@Param("noteId") UUID noteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from Note n
            where n.id = :noteId
            and n.owner.id = :ownerId
            and n.deletedAt is null
            """)
    Optional<Note> findActiveByIdAndOwnerForUpdate(
            @Param("noteId") UUID noteId,
            @Param("ownerId") UUID ownerId
    );

    @Query("""
            select new com.abhout.cortex_app_be.note.dtos.NoteSummaryDto(
                n.id, n.title, n.createdAt, n.updatedAt
            )
            from Note n
            where n.owner.id = :ownerId
            and n.deletedAt is null
            order by n.updatedAt desc, n.id desc
            """)
    List<NoteSummaryDto> findFirstPageByOwner(
            @Param("ownerId") UUID ownerId,
            Pageable pageable
    );

    @Query("""
            select new com.abhout.cortex_app_be.note.dtos.NoteSummaryDto(
                n.id, n.title, n.createdAt, n.updatedAt
            )
            from Note n
            where n.owner.id = :ownerId
            and n.deletedAt is null
            and (n.updatedAt < :cursorUpdatedAt
                 or (n.updatedAt = :cursorUpdatedAt and n.id < :cursorId))
            order by n.updatedAt desc, n.id desc
            """)
    List<NoteSummaryDto> findPageByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("cursorUpdatedAt") Instant cursorUpdatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<Note> findByOwnerIdAndUpdatedAtAfter(UUID ownerId, Instant since);
}
