package com.abhout.cortex_app_be.note.repositories;

import com.abhout.cortex_app_be.note.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByOwnerId(UUID ownerId);
}
