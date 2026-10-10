package com.abhout.cortex_app_be.sync.services;

import com.abhout.cortex_app_be.jobs.services.JobPublisher;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.abhout.cortex_app_be.sync.dtos.PushOutcome.*;

@Service
public class SyncService {

    // Must exceed the longest push/update transaction (stamp-to-commit time).
    private static final Duration PULL_OVERLAP = Duration.ofSeconds(60);

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final JobPublisher jobPublisher;

    public SyncService(
            NoteRepository noteRepository,
            UserRepository userRepository,
            CacheManager cacheManager,
            JobPublisher jobPublisher) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.jobPublisher = jobPublisher;
    }

    // Key must match NoteService's @Cacheable key: "#ownerId + ':' + #noteId"
    private void evictNoteFromCache(UUID ownerId, UUID noteId) {
        Cache cache = cacheManager.getCache("notes");
        if (cache != null) {
            cache.evict(ownerId + ":" + noteId);
        }
    }

    @Transactional
    public PushResponse push(UUID ownerId, PushRequest request) {
        List<NotePushItem> items = request.notes();
        PushResultItem[] results = new PushResultItem[items.size()];
        // pushOne row-locks each note. Taking the locks in a fixed (id) order means two
        // concurrent batches touching the same notes queue up instead of deadlocking.
        // Results still come back in request order.
        IntStream.range(0, items.size()).boxed()
                .sorted(Comparator.comparing(i -> items.get(i).id()))
                .forEach(i -> results[i] = pushOne(ownerId, items.get(i)));
        return new PushResponse(List.of(results));
    }

    private PushResultItem pushOne(UUID ownerId, NotePushItem item) {
        Optional<Note> existing = noteRepository.findByIdForUpdate(item.id());

        if (existing.isEmpty()) {
            if (item.deleted()) {
                // Created and deleted while offline; the server never had it.
                return new PushResultItem(item.id(), null, null, null, null, null, true);
            }
            User owner = userRepository.getReferenceById(ownerId);
            Note newNote = new Note(orEmpty(item.title()), orEmpty(item.body()), owner);
            newNote.setId(item.id());
            noteRepository.save(newNote);
            jobPublisher.publishEmbedJob(newNote);
            return new PushResultItem(item.id(), ACCEPTED, ACCEPTED,
                    newNote.getTitleUpdatedAt(), newNote.getBodyUpdatedAt(), null, false);
        }

        Note note = existing.get();

        if (!note.getOwner().getId().equals(ownerId)) {
            return new PushResultItem(item.id(), REJECTED_STALE, REJECTED_STALE, null, null, null, false);
        }

        boolean titleSent = item.title() != null;
        boolean bodySent = item.body() != null;

        // Delete wins: edits to a tombstoned note are rejected, never resurrected.
        if (note.isDeleted()) {
            return new PushResultItem(item.id(),
                    titleSent ? REJECTED_DELETED : null,
                    bodySent ? REJECTED_DELETED : null,
                    null, null, null, true);
        }

        if (item.deleted()) {
            note.setDeletedAt(Note.versionStamp());
            noteRepository.save(note);
            jobPublisher.publishDeleteJob(note.getId(), ownerId);
            evictNoteFromCache(ownerId, note.getId());
            return new PushResultItem(item.id(), null, null, null, null, null, true);
        }

        // A sent field is applied only if the device's base still equals the stored stamp.
        // A null base can't prove the device saw the current value, so it never matches.
        boolean titleBaseMatches = titleSent && note.getTitleUpdatedAt().equals(item.titleBaseUpdatedAt());
        boolean bodyBaseMatches = bodySent && note.getBodyUpdatedAt().equals(item.bodyBaseUpdatedAt());

        boolean titleConflict = titleSent && !titleBaseMatches && !item.title().equals(note.getTitle());
        boolean bodyConflict = bodySent && !bodyBaseMatches && !item.body().equals(note.getBody());

        // Unchanged content keeps its stamp, so other devices' bases stay valid.
        Instant now = Note.versionStamp();
        boolean changed = false;
        if (titleBaseMatches && !item.title().equals(note.getTitle())) {
            note.setTitle(item.title());
            note.setTitleUpdatedAt(now);
            changed = true;
        }
        if (bodyBaseMatches && !item.body().equals(note.getBody())) {
            note.setBody(item.body());
            note.setBodyUpdatedAt(now);
            changed = true;
        }
        if (changed) {
            noteRepository.save(note);
            jobPublisher.publishEmbedJob(note);
            evictNoteFromCache(ownerId, note.getId());
        }

        UUID conflictNoteId = null;
        if (titleConflict || bodyConflict) {
            // The copy is the device's version: its sent fields, plus the server's
            // current value for any field it didn't send (it didn't change those).
            Note conflictCopy = new Note(
                    titleSent ? item.title() : note.getTitle(),
                    bodySent ? item.body() : note.getBody(),
                    note.getOwner());
            conflictCopy.setConflictOf(note.getId());
            conflictCopy.setConflictField(
                    titleConflict && bodyConflict ? "title,body" :
                            titleConflict ? "title" : "body"
            );
            noteRepository.save(conflictCopy);
            jobPublisher.publishEmbedJob(conflictCopy);
            conflictNoteId = conflictCopy.getId();
        }

        PushOutcome titleOutcome = outcome(titleSent, titleBaseMatches, titleConflict);
        PushOutcome bodyOutcome = outcome(bodySent, bodyBaseMatches, bodyConflict);
        return new PushResultItem(
                item.id(),
                titleOutcome,
                bodyOutcome,
                deviceMatchesServer(titleOutcome) ? note.getTitleUpdatedAt() : null,
                deviceMatchesServer(bodyOutcome) ? note.getBodyUpdatedAt() : null,
                conflictNoteId,
                false
        );
    }

    private static PushOutcome outcome(boolean sent, boolean baseMatches, boolean conflict) {
        if (!sent) {
            return null;
        }
        if (baseMatches) {
            return ACCEPTED;
        }
        return conflict ? CONFLICT : REJECTED_STALE;
    }

    // After ACCEPTED or REJECTED_STALE the device's field content equals the server's.
    private static boolean deviceMatchesServer(PushOutcome outcome) {
        return outcome == ACCEPTED || outcome == REJECTED_STALE;
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    @Transactional(readOnly = true)
    public PullResponse pull(UUID ownerId, Instant since) {
        // Take the cursor before querying, and step it back by PULL_OVERLAP. updatedAt is
        // stamped when a transaction flushes, but it commits later; a change stamped before
        // this pull and committed after the query would otherwise fall behind the cursor
        // and never be pulled. Re-sent notes are harmless: clients apply them idempotently.
        Instant nextSince = Instant.now().minus(PULL_OVERLAP);
        List<Note> notes = noteRepository.findByOwnerIdAndUpdatedAtAfter(ownerId, since);
        List<NoteDetailDto> res = new ArrayList<>();
        for (Note note : notes) {
            res.add(NoteDetailDto.from(note));
        }
        return new PullResponse(res, nextSince);
    }
}
