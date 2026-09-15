package com.abhout.cortex_app_be.search.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.search.dtos.SearchResultDto;
import com.abhout.cortex_app_be.search.services.SearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/search")
@Validated
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<SearchResultDto>>> search(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam("q") @NotBlank String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "0") int limit
    ) {
        CursorPage<SearchResultDto> results = searchService.search(ownerId, q, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
