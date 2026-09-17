package com.abhout.cortex_app_be.note.dtos;

import com.abhout.cortex_app_be.note.entities.Attachment;
import com.abhout.cortex_app_be.note.entities.Note;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteDetailDto(
        UUID id,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt,
        Instant titleUpdatedAt,
        Instant bodyUpdatedAt,
        List<AttachmentSummaryDto> attachments,
        UUID conflictOf,
        String conflictField
) {
    public static NoteDetailDto from(Note note) {
        List<AttachmentSummaryDto> attachmentDtos = note.getAttachments().stream()
                .map(AttachmentSummaryDto::from)
                .toList();

        return new NoteDetailDto(
                note.getId(),
                note.getTitle(),
                note.getBody(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getTitleUpdatedAt(),
                note.getBodyUpdatedAt(),
                attachmentDtos,
                note.getConflictOf(),
                note.getConflictField()
        );
    }
}
