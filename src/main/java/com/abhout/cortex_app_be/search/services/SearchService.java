package com.abhout.cortex_app_be.search.services;

import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.search.dtos.SearchCursor;
import com.abhout.cortex_app_be.search.dtos.SearchResultDto;
import com.abhout.cortex_app_be.search.dtos.SearchResultProjection;
import com.abhout.cortex_app_be.search.repositories.NoteSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private final NoteSearchRepository noteSearchRepository;

    public SearchService(NoteSearchRepository noteSearchRepository) {
        this.noteSearchRepository = noteSearchRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<SearchResultDto> search(UUID ownerId, String query, String cursor, int limit) {
        int clampedLimit = clampLimit(limit);
        Pageable pageable = PageRequest.of(0, clampedLimit + 1);

        SearchCursor decodedCursor = (cursor != null && !cursor.isBlank())
                ? SearchCursor.decode(cursor)
                : null;

        List<SearchResultProjection> fetched = decodedCursor == null
                ? noteSearchRepository.searchFirstPage(ownerId, query, pageable)
                : noteSearchRepository.searchNextPage(
                ownerId,
                query,
                decodedCursor.rank(),
                decodedCursor.id(),
                pageable
        );

        boolean hasNext = fetched.size() > clampedLimit;
        List<SearchResultProjection> pageContent = hasNext ? fetched.subList(0, clampedLimit) : fetched;

        String nextCursor = hasNext
                ? new SearchCursor(
                pageContent.get(pageContent.size() - 1).getRank(),
                pageContent.get(pageContent.size() - 1).getId()
        ).encode()
                : null;

        List<SearchResultDto> items = pageContent.stream()
                .map(p -> new SearchResultDto(
                        p.getId(),
                        p.getTitle(),
                        p.getCreatedAt(),
                        p.getUpdatedAt(),
                        p.getRank()
                )).toList();

        return new CursorPage<>(items, nextCursor);
    }

    private int clampLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requested, MAX_LIMIT);
    }
}
