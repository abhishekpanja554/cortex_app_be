package com.abhout.cortex_app_be.note.services;

import com.abhout.cortex_app_be.config.AttachmentProperties;
import com.abhout.cortex_app_be.note.dtos.AttachmentSummaryDto;
import com.abhout.cortex_app_be.note.entities.Attachment;
import com.abhout.cortex_app_be.note.exceptions.AttachmentIdTakenException;
import com.abhout.cortex_app_be.note.exceptions.AttachmentNotFoundException;
import com.abhout.cortex_app_be.note.exceptions.AttachmentTooLargeException;
import com.abhout.cortex_app_be.note.exceptions.UnsupportedAttachmentTypeException;
import com.abhout.cortex_app_be.note.repositories.AttachmentRepository;
import com.abhout.cortex_app_be.user.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Stores image bytes on disk under <dir>/<ownerId>/<attachmentId>; the row only holds metadata.
@Service
public class AttachmentService {
    private static final int SNIFF_BYTES = 12;
    private static final Set<String> HEIF_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1");

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final Path root;
    private final long maxBytes;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            UserRepository userRepository,
            AttachmentProperties properties
    ) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.root = Path.of(properties.getDir()).toAbsolutePath().normalize();
        this.maxBytes = properties.getMaxSize().toBytes();
    }

    public record PutResult(AttachmentSummaryDto attachment, boolean created) {}

    public record StoredFile(Path path, String contentType, long sizeBytes) {}

    // Idempotent: re-uploading an id the caller already owns returns the stored row.
    // Not @Transactional, so a concurrent upload of the same id fails fast on the primary
    // key and can be answered with the row that won.
    public PutResult put(UUID ownerId, UUID attachmentId, long contentLength, InputStream body) throws IOException {
        Optional<PutResult> existing = existing(ownerId, attachmentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (contentLength > maxBytes) {
            throw new AttachmentTooLargeException(maxBytes);
        }

        // The type comes from the file's own bytes, not the Content-Type header, so
        // nothing but an image is ever served back from this endpoint.
        byte[] header = body.readNBytes(SNIFF_BYTES);
        String contentType = sniffImageType(header);
        if (contentType == null) {
            throw new UnsupportedAttachmentTypeException();
        }

        Path ownerDir = root.resolve(ownerId.toString());
        Files.createDirectories(ownerDir);
        Path temp = Files.createTempFile(ownerDir, attachmentId + "-", ".part");
        long size;
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                out.write(header);
                size = header.length + copyWithLimit(body, out, maxBytes - header.length);
            }
            // Readers never see a half-written file.
            Files.move(temp, ownerDir.resolve(attachmentId.toString()),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }

        Attachment attachment = new Attachment(
                attachmentId,
                userRepository.getReferenceById(ownerId),
                attachmentId + extensionFor(contentType),
                contentType,
                size,
                ownerId + "/" + attachmentId
        );
        try {
            attachmentRepository.saveAndFlush(attachment);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another upload of the same id (or the row existed but its
            // file was missing, which the write above has now repaired).
            return existing(ownerId, attachmentId).orElseThrow(() -> e);
        }
        return new PutResult(AttachmentSummaryDto.from(attachment), true);
    }

    // Someone else's id is reported as missing, so ids can't be probed.
    public StoredFile get(UUID ownerId, UUID attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndOwnerId(attachmentId, ownerId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
        Path path = resolve(attachment);
        if (path == null || !Files.isRegularFile(path)) {
            throw new AttachmentNotFoundException(attachmentId);
        }
        return new StoredFile(path, attachment.getContentType(), attachment.getSizeBytes());
    }

    private Optional<PutResult> existing(UUID ownerId, UUID attachmentId) {
        Optional<Attachment> own = attachmentRepository.findByIdAndOwnerId(attachmentId, ownerId);
        if (own.isPresent()) {
            Path path = resolve(own.get());
            // Row without its file (e.g. a database restored without the volume): let the
            // caller write the bytes again.
            if (path == null || !Files.isRegularFile(path)) {
                return Optional.empty();
            }
            return Optional.of(new PutResult(AttachmentSummaryDto.from(own.get()), false));
        }
        if (attachmentRepository.existsById(attachmentId)) {
            throw new AttachmentIdTakenException(attachmentId);
        }
        return Optional.empty();
    }

    private Path resolve(Attachment attachment) {
        Path path = root.resolve(attachment.getStoragePath()).normalize();
        return path.startsWith(root) ? path : null;
    }

    private long copyWithLimit(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new AttachmentTooLargeException(maxBytes);
            }
            out.write(buffer, 0, read);
        }
        return total;
    }

    static String sniffImageType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (h.length >= 8 && Arrays.equals(Arrays.copyOf(h, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "image/png";
        }
        if (h.length >= 12 && ascii(h, 0, 4).equals("RIFF") && ascii(h, 8, 12).equals("WEBP")) {
            return "image/webp";
        }
        if (h.length >= 12 && ascii(h, 4, 8).equals("ftyp") && HEIF_BRANDS.contains(ascii(h, 8, 12))) {
            return "image/heic";
        }
        return null;
    }

    private static String ascii(byte[] bytes, int from, int to) {
        return new String(bytes, from, to - from, StandardCharsets.US_ASCII);
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            default -> ".jpg";
        };
    }
}
