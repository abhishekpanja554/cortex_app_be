package com.abhout.cortex_app_be.sync.services;

import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;
import com.abhout.cortex_app_be.note.entities.Note;
import com.abhout.cortex_app_be.note.repositories.NoteRepository;
import com.abhout.cortex_app_be.sync.dtos.*;
import com.abhout.cortex_app_be.user.entities.User;
import com.abhout.cortex_app_be.user.repositories.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.abhout.cortex_app_be.sync.dtos.PushOutcome.ACCEPTED;

@Service
public class SyncService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public SyncService(NoteRepository noteRepository, UserRepository userRepository, CacheManager cacheManager) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    private void evictNoteFromCache(UUID noteId) {
        Cache cache = cacheManager.getCache("notes");
        if (cache != null) {
            cache.evict(noteId);
        }
    }

    @Transactional
    public PushResponse push(UUID ownerId, PushRequest request) {
        List<PushResultItem> results = new ArrayList<>();

        for (NotePushItem item : request.notes()) {
            Optional<Note> existing = noteRepository.findById(item.id());

            if (existing.isEmpty()) {
                User owner = userRepository.getReferenceById(ownerId);
                Note newNote = new Note(item.title(), item.body(), owner);
                newNote.setId(item.id());
                noteRepository.save(newNote);
                results.add(new PushResultItem(item.id(), ACCEPTED, ACCEPTED, null));
                continue;
            }

            Note note = existing.get();

            if (!note.getOwner().getId().equals(ownerId)) {
                results.add(new PushResultItem(item.id(), PushOutcome.REJECTED_STALE, PushOutcome.REJECTED_STALE, null));
                continue;
            }

            boolean titleBaseMatches = item.titleBaseUpdatedAt().equals(note.getTitleUpdatedAt());
            boolean bodyBaseMatches = item.bodyBaseUpdatedAt().equals(note.getBodyUpdatedAt());

            boolean titleNoop = !titleBaseMatches && item.title().equals(note.getTitle());
            boolean bodyNoop = !bodyBaseMatches && item.body().equals(note.getBody());

            boolean titleConflict = !titleBaseMatches && !titleNoop;
            boolean bodyConflict = !bodyBaseMatches && !bodyNoop;

            if (titleBaseMatches) {
                note.setTitle(item.title());
                note.setTitleUpdatedAt(Instant.now());   //server-assigned, not client-claimed
            }
            if (bodyBaseMatches) {
                note.setBody(item.body());
                note.setBodyUpdatedAt(Instant.now());
            }
            if (titleBaseMatches || bodyBaseMatches) {
                noteRepository.save(note);
                evictNoteFromCache(note.getId());
            }
            if (titleConflict || bodyConflict) {
                noteRepository.save(note);
                evictNoteFromCache(note.getId());
            }

            UUID conflictNoteId = null;
            if (titleConflict || bodyConflict) {
                Note conflictCopy = new Note(item.title(), item.body(),
                        note.getOwner());
                conflictCopy.setConflictOf(note.getId());
                conflictCopy.setConflictField(
                        titleConflict && bodyConflict ? "title,body" :
                                titleConflict ? "title" : "body"
                );
                noteRepository.save(conflictCopy);
                conflictNoteId = conflictCopy.getId();
            }

            results.add(new PushResultItem(
                    item.id(),
                    titleBaseMatches ? PushOutcome.ACCEPTED : (titleConflict ? PushOutcome.CONFLICT
                            : PushOutcome.REJECTED_STALE),
                    bodyBaseMatches ? ACCEPTED : (bodyConflict ? PushOutcome.CONFLICT :
                            PushOutcome.REJECTED_STALE),
                    conflictNoteId
            ));
        }

        return new PushResponse(results);
    }

    @Transactional
    public PullResponse pull(UUID ownerId, Instant since) {
        List<Note> notes = noteRepository.findByOwnerIdAndUpdatedAtAfter(ownerId, since);
        List<NoteDetailDto> res = new ArrayList<>();
        for(Note note : notes){
            res.add(NoteDetailDto.from(note));
        }
        return new PullResponse(res,Instant.now());
    }
}
