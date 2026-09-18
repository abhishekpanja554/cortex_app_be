package com.abhout.cortex_app_be.jobs.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.jobs.dtos.JobFailRequest;
import com.abhout.cortex_app_be.jobs.services.InternalJobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/jobs")
public class InternalJobController {
    private final InternalJobService internalJobService;

    public InternalJobController(InternalJobService internalJobService) {
        this.internalJobService = internalJobService;
    }

    @PostMapping("/{jobId}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(@PathVariable UUID jobId) {
        internalJobService.complete(jobId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{jobId}/fail")
    public ResponseEntity<ApiResponse<Void>> fail(@PathVariable UUID jobId, @Valid @RequestBody JobFailRequest request) {
        internalJobService.fail(jobId, request.errorMessage());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
