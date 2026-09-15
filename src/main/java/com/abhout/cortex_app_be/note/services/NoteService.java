package com.abhout.cortex_app_be.note.services;

import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.note.dtos.*;
import com.abhout.cortex_app_be.note.entities.Note;
import com.abhout.cortex_app_be.note.exceptions.NoteNotFoundException;
import com.abhout.cortex_app_be.note.repositories.NoteRepository;
import com.abhout.cortex_app_be.user.entities.User;
import com.abhout.cortex_app_be.user.repositories.UserRepository;
import org.springframework.data.crossstore.ChangeSetPersister;
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

    public NoteService(
            NoteRepository noteRepository,
            UserRepository userRepository
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
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
    public NoteDetailDto get(UUID ownerId, UUID noteId) {
        Note note = noteRepository.findByIdAndOwnerId(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        return NoteDetailDto.from(note);
    }

    @Transactional
    public NoteDetailDto create(UUID ownerId, NoteCreateRequest request) {
        User owner = userRepository.getReferenceById(ownerId);

        Note note = new Note(request.title(), request.body(), owner);
        noteRepository.save(note);

        return NoteDetailDto.from(note);
    }

    @Transactional
    public NoteDetailDto update(UUID ownerId, UUID noteId, NoteUpdateRequest request) {
        Note note = noteRepository.findByIdAndOwnerId(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        note.setTitle(request.title());
        note.setBody(request.body());
        noteRepository.save(note);

        return NoteDetailDto.from(note);
    }

    @Transactional
    public void delete(UUID ownerId, UUID noteId) {
        Note note = noteRepository.findByIdAndOwnerId(noteId, ownerId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        noteRepository.delete(note);
    }

    private int clampLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }
}
