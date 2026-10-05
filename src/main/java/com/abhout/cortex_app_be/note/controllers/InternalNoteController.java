package com.abhout.cortex_app_be.note.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.note.dtos.NoteDetailDto;
import com.abhout.cortex_app_be.note.services.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/notes")
public class InternalNoteController {
    private final NoteService noteService;

    public InternalNoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteDetailDto>> getInternal(
            @PathVariable UUID noteId,
            @RequestParam UUID ownerId
    ){
        NoteDetailDto res = noteService.get(ownerId, noteId);
        return  ResponseEntity.ok(ApiResponse.success(res));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteInternal(
            @PathVariable UUID noteId,
            @RequestParam UUID ownerId
    ) {
        noteService.delete(ownerId, noteId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
