package com.abhout.cortex_app_be.note.repositories;

import com.abhout.cortex_app_be.note.dtos.NoteSummaryDto;
import com.abhout.cortex_app_be.note.entities.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    Optional<Note> findByIdAndOwnerId(UUID noteId, UUID ownerId);

    @Query("""
            select new com.abhout.cortex_app_be.note.dtos.NoteSummaryDto(
                n.id, n.title, n.createdAt, n.updatedAt
            )
            from Note n
            where n.owner.id = :ownerId
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
}
