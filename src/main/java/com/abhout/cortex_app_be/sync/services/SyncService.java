package com.abhout.cortex_app_be.sync.services;

import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;
import com.abhout.cortex_app_be.note.entities.Note;
import com.abhout.cortex_app_be.note.exceptions.NoteNotFoundException;
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
                results.add(new PushResultItem(item.id(), PushOutcome.ACCEPTED));
                continue;
            }

            Note note = existing.get();

            if (!note.getOwner().getId().equals(ownerId)) {
                results.add(new PushResultItem(item.id(), PushOutcome.REJECTED_STALE));
                continue;
            }

            if (item.clientUpdatedAt().isAfter(note.getUpdatedAt())) {
                note.setTitle(item.title());
                note.setBody(item.body());
                noteRepository.save(note);
                evictNoteFromCache(note.getId());
                results.add(new PushResultItem(item.id(), PushOutcome.ACCEPTED));
            } else {
                results.add(new PushResultItem(item.id(), PushOutcome.REJECTED_STALE));
            }
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
