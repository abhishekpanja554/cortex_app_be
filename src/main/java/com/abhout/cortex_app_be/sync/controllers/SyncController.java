package com.abhout.cortex_app_be.sync.controllers;

import com.abhout.cortex_app_be.common.ApiResponse;
import com.abhout.cortex_app_be.sync.dtos.PullResponse;
import com.abhout.cortex_app_be.sync.dtos.PushRequest;
import com.abhout.cortex_app_be.sync.dtos.PushResponse;
import com.abhout.cortex_app_be.sync.services.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService){
        this.syncService = syncService;
    }

    @GetMapping("/pull")
    public ResponseEntity<ApiResponse<PullResponse>> pull(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam(required = false,defaultValue = "1970-01-01T00:00:00Z") Instant since
    ){
        PullResponse response = syncService.pull(ownerId, since);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/push")
    public ResponseEntity<ApiResponse<PushResponse>> push(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody PushRequest request
    ){
        PushResponse response = syncService.push(ownerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
