package com.abhout.cortex_app_be.note.dtos;

import com.abhout.cortex_app_be.note.entities.Attachment;

import java.util.UUID;

public record AttachmentSummaryDto(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes
) {
    public static AttachmentSummaryDto from(Attachment attachment) {
        return new AttachmentSummaryDto(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }
}
