package com.abhout.cortex_app_be.auth.services;

import com.abhout.cortex_app_be.auth.RotationResult;
import com.abhout.cortex_app_be.auth.dtos.AuthResponse;
import com.abhout.cortex_app_be.auth.dtos.LoginRequest;
import com.abhout.cortex_app_be.auth.dtos.RefreshRequest;
import com.abhout.cortex_app_be.auth.dtos.RegisterRequest;
import com.abhout.cortex_app_be.auth.exceptions.EmailAlreadyInUseException;
import com.abhout.cortex_app_be.auth.exceptions.InvalidCredentialsException;
import com.abhout.cortex_app_be.auth.exceptions.InvalidRefreshTokenException;
import com.abhout.cortex_app_be.user.entities.User;
import com.abhout.cortex_app_be.user.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JWTService jwtService;

    AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            JWTService jwtService
    ){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest registerRequest){
        Optional<User> user = userRepository.findByEmail(registerRequest.email());
        if (user.isPresent()){
            throw new EmailAlreadyInUseException("Email already in use");
        }
        String passwordHash = passwordEncoder.encode(registerRequest.password());
        User newUser = new User(registerRequest.email(), passwordHash);
        userRepository.save(newUser);
        String accessToken = jwtService.generateAccessToken(newUser);
        String refreshToken = refreshTokenService.issue(newUser, UUID.randomUUID());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest loginRequest){
        Optional<User> user = userRepository.findByEmail(loginRequest.email());
        if (user.isEmpty()){
            throw new InvalidCredentialsException("Incorrect Credentials");
        }
        if (!passwordEncoder.matches(loginRequest.password(), user.get().getPasswordHash())){
            throw new InvalidCredentialsException("Incorrect Credentials");
        }
        String accessToken = jwtService.generateAccessToken(user.get());
        String refreshToken = refreshTokenService.issueNewFamily(user.get());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest refreshRequest){
        RotationResult rotationResult = refreshTokenService.rotate(refreshRequest.refreshToken());
        String accessToken = jwtService.generateAccessToken(rotationResult.user());
        return new AuthResponse(accessToken, rotationResult.rawToken());
    }
}
