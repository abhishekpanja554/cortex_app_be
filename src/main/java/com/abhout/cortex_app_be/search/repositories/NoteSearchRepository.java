package com.abhout.cortex_app_be.search.repositories;

import com.abhout.cortex_app_be.note.entities.Note;
import com.abhout.cortex_app_be.search.dtos.SearchResultProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteSearchRepository extends JpaRepository<Note, UUID> {
    @Query(value = """
            SELECT
                n.id AS "id",
                n.title AS "title",
                n.created_at AS "createdAt",
                n.updated_at AS "updatedAt",
                ts_rank(n.search_vector, websearch_to_tsquery('english', :query)) AS "rank"
            FROM notes n
            WHERE n.owner_id = :ownerId
            AND n.search_vector @@ websearch_to_tsquery('english', :query)
            ORDER BY rank DESC, n.id DESC
            """, nativeQuery = true)
    List<SearchResultProjection> searchFirstPage(
            @Param("ownerId") UUID ownerId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                n.id AS "id",
                n.title AS "title",
                n.created_at AS "createdAt",
                n.updated_at AS "updatedAt",
                ts_rank(n.search_vector, websearch_to_tsquery('english', :query)) AS "rank"
            FROM notes n
            WHERE n.owner_id = :ownerId
            AND n.search_vector @@ websearch_to_tsquery('english', :query)
            AND (
                ts_rank(n.search_vector, websearch_to_tsquery('english', :query)) < :cursorRank
                OR (
                    ts_rank(n.search_vector, websearch_to_tsquery('english', :query)) = :cursorRank
                    AND n.id < :cursorId
                )
            )
            ORDER BY rank DESC, n.id DESC
            """, nativeQuery = true)
    List<SearchResultProjection> searchNextPage(
            @Param("ownerId") UUID ownerId,
            @Param("query") String query,
            @Param("cursorRank") double cursorRank,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
