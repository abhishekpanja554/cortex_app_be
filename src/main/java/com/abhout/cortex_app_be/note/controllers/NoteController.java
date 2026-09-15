package com.abhout.cortex_app_be.note.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.note.dtos.NoteCreateRequest;
import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;
import com.abhout.cortex_app_be.note.dtos.NoteSummaryDto;
import com.abhout.cortex_app_be.note.dtos.NoteUpdateRequest;
import com.abhout.cortex_app_be.note.services.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notes")
public class NoteController {
    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<NoteSummaryDto>>> getNotes(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "0") int limit
    ){
        CursorPage<NoteSummaryDto> page = noteService.list(ownerId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteDetailDto>> getNote(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID noteId
    ){
        NoteDetailDto note = noteService.get(ownerId, noteId);
        return ResponseEntity.ok(ApiResponse.success(note));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteDetailDto>> createNote(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody NoteCreateRequest note
    ){
        NoteDetailDto createdNote = noteService.create(ownerId, note);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(createdNote));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteDetailDto>> updateNote(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteUpdateRequest note
    ){
        NoteDetailDto updatedNote = noteService.update(ownerId, noteId, note);
        return ResponseEntity.ok(ApiResponse.success(updatedNote));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID noteId
    ){
        noteService.delete(ownerId, noteId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success());
    }
}
