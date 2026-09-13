package com.abhout.cortex_app_be.auth.services;

import com.abhout.cortex_app_be.auth.entities.RefreshToken;
import com.abhout.cortex_app_be.auth.exceptions.InvalidRefreshTokenException;
import com.abhout.cortex_app_be.auth.repositories.RefreshTokenRepository;
import com.abhout.cortex_app_be.config.JWTProperties;
import com.abhout.cortex_app_be.user.entities.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final int RAW_TOKEN_BYTES = 32; // 256 bits

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JWTProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public String issueNewFamily(User user){
        return issue(user, UUID.randomUUID());
    }

    public String issue(User user, UUID familyId){
        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                familyId,
                Instant.now().plus(jwtProperties.getRefreshTokenTtl())
        );
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public String rotate(String presentedRawToken) {
        String presentedHash = sha256(presentedRawToken);

        RefreshToken found = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (found.isRevoked()) {
            refreshTokenRepository.revokeAllByFamilyId(found.getFamilyId(), Instant.now());
            throw new InvalidRefreshTokenException("Refresh token reuse detected; family revoked");
        }

        if (found.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        found.revoke();
        refreshTokenRepository.save(found);
        return issue(found.getUser(), found.getFamilyId());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
