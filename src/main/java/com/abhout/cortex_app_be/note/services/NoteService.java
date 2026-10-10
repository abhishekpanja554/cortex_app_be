package com.abhout.cortex_app_be.note.services;

import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.jobs.entities.JobType;
import com.abhout.cortex_app_be.jobs.services.JobPublisher;
import com.abhout.cortex_app_be.note.dtos.*;
import com.abhout.cortex_app_be.note.entities.Note;
import com.abhout.cortex_app_be.note.exceptions.NoteNotFoundException;
import com.abhout.cortex_app_be.note.repositories.NoteRepository;
import com.abhout.cortex_app_be.user.entities.User;
import com.abhout.cortex_app_be.user.repositories.UserRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NoteService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final JobPublisher jobPublisher;

    public NoteService(
            NoteRepository noteRepository,
            UserRepository userRepository,
            JobPublisher jobPublisher
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.jobPublisher = jobPublisher;
    }

    public CursorPage<NoteSummaryDto> list(
            UUID ownerId,
            String cursor,
            int limit
    ){
        int clampedLimit = clampLimit(limit);
        Pageable pageable = PageRequest.of(0, clampedLimit + 1);
        NoteCursor decodedCursor = (cursor != null && !cursor.isBlank())?
                NoteCursor.decode(cursor) : null;

        Instant cursorUpdatedAt = decodedCursor != null ?
                decodedCursor.updatedAt() : null;
        UUID cursorId = decodedCursor != null ?
                decodedCursor.id() : null;

        List<NoteSummaryDto> fetched = decodedCursor == null ?
                noteRepository.findFirstPageByOwner(ownerId, pageable) :
                noteRepository.findPageByOwner(
                        ownerId,
                        cursorUpdatedAt,
                        cursorId,
                        pageable
                );

        boolean hasNext = fetched.size() > clampedLimit;
        List<NoteSummaryDto> pageContent = hasNext ? fetched.subList(0, clampedLimit) : fetched;
        String nextCursor = hasNext
                ? new NoteCursor(
                pageContent.get(pageContent.size() - 1).updatedAt(),
                pageContent.get(pageContent.size() - 1).id()
        ).encode()
                : null;

        List<NoteSummaryDto> items = pageContent.stream().toList();
        return new CursorPage<>(items, nextCursor);
    }

    @Transactional
    @Cacheable(value = "notes", key = "#ownerId + ':' + #noteId")
    public NoteDetailDto get(UUID ownerId, UUID noteId) {
        Note note = noteRepository.findByIdAndOwnerIdAndDeletedAtIsNull(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        return NoteDetailDto.from(note);
    }

    @Transactional
    public NoteDetailDto create(UUID ownerId, NoteCreateRequest request) {
        User owner = userRepository.getReferenceById(ownerId);

        Note note = new Note(request.title(), request.body(), owner);
        noteRepository.save(note);
        jobPublisher.publishEmbedJob(note);
        return NoteDetailDto.from(note);
    }

    @Transactional
    @CacheEvict(value = "notes", key = "#ownerId + ':' + #noteId")
    public NoteDetailDto update(UUID ownerId, UUID noteId, NoteUpdateRequest request) {
        Note note = noteRepository.findActiveByIdAndOwnerForUpdate(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        // Only bump a field's version stamp if its content actually changed. PUT always carries
        // both fields; bumping both would make a device's pending edit to the untouched field
        // fail its base check on the next sync push and spawn a false conflict copy.
        boolean titleChanged = !note.getTitle().equals(request.title());
        boolean bodyChanged = !note.getBody().equals(request.body());
        if (!titleChanged && !bodyChanged) {
            return NoteDetailDto.from(note);
        }

        Instant now = Note.versionStamp();
        if (titleChanged) {
            note.setTitle(request.title());
            note.setTitleUpdatedAt(now);
        }
        if (bodyChanged) {
            note.setBody(request.body());
            note.setBodyUpdatedAt(now);
        }
        noteRepository.save(note);
        jobPublisher.publishEmbedJob(note);
        return NoteDetailDto.from(note);
    }

    @Transactional
    @CacheEvict(value = "notes", key = "#ownerId + ':' + #noteId")
    public void delete(UUID ownerId, UUID noteId) {
        Note note = noteRepository.findActiveByIdAndOwnerForUpdate(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        // Soft delete: the row stays as a tombstone so /sync/pull can tell other devices,
        // and so a later sync push for this ID is rejected instead of re-creating the note.
        note.setDeletedAt(Note.versionStamp());
        noteRepository.save(note);
        jobPublisher.publishDeleteJob(note.getId(), ownerId);
    }

    private int clampLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }
}
