package com.abhout.cortex_app_be.note.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.note.dtos.AttachmentSummaryDto;
import com.abhout.cortex_app_be.note.services.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    // Raw image bytes as the request body (not multipart), streamed straight to disk.
    @PutMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<AttachmentSummaryDto>> upload(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID attachmentId,
            HttpServletRequest request
    ) throws IOException {
        AttachmentService.PutResult result = attachmentService.put(
                ownerId, attachmentId, request.getContentLengthLong(), request.getInputStream());
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.attachment()));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID attachmentId
    ) {
        AttachmentService.StoredFile file = attachmentService.get(ownerId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                // The bytes behind an id never change.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .body(new FileSystemResource(file.path()));
    }
}
