package com.abhout.cortex_app_be.note.repositories;

import com.abhout.cortex_app_be.note.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    Optional<Attachment> findByIdAndOwnerId(UUID id, UUID ownerId);
}
