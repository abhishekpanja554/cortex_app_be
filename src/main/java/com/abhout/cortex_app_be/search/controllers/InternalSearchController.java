package com.abhout.cortex_app_be.search.controllers;

import com.abhout.cortex_app_be.common.CursorPage;
import com.abhout.cortex_app_be.search.dtos.InternalSearchRequest;
import com.abhout.cortex_app_be.search.dtos.SearchResultDto;
import com.abhout.cortex_app_be.search.services.SearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/search")
public class InternalSearchController {
    private final SearchService searchService;

    public InternalSearchController(SearchService searchService){
        this.searchService = searchService;
    }

    @PostMapping
    public List<SearchResultDto> searchInternal(
            @RequestBody InternalSearchRequest request
    ){
        CursorPage<SearchResultDto> res = searchService.search(
                request.ownerId(),
                request.query(),
                null,
                request.limit()
        );
        return res.items();
    }
}
