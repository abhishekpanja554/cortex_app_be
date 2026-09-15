package com.abhout.cortex_app_be.auth.controllers;

import com.abhout.cortex_app_be.auth.dtos.AuthResponse;
import com.abhout.cortex_app_be.auth.dtos.LoginRequest;
import com.abhout.cortex_app_be.auth.dtos.RefreshRequest;
import com.abhout.cortex_app_be.auth.dtos.RegisterRequest;
import com.abhout.cortex_app_be.auth.services.AuthService;
import com.abhout.cortex_app_be.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest refreshRequest
    ) {
        AuthResponse response = authService.refresh(refreshRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
