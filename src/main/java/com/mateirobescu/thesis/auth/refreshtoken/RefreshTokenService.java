package com.mateirobescu.thesis.auth.refreshtoken;

import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.exception.NotFoundException;
import com.mateirobescu.thesis.exception.SessionExpiredException;
import com.mateirobescu.thesis.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Long tokenValidityDays;

    public RefreshTokenService(
            @Value("${refresh-token-validity:30}") Long tokenValidityDays,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.tokenValidityDays = tokenValidityDays;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issueRefreshToken(User user) {

        String token = generateSecureRandomToken();
        String tokenHash = hashToken(token);
        Instant expirationTimestamp = Instant.now().plus(tokenValidityDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expirationTimestamp)
                .build();

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void revokeRefreshToken(RefreshToken token) {
        int rowsUpdated = refreshTokenRepository.revokeIfNotAlreadyRevoked(token.getId(), Instant.now());
        if(rowsUpdated == 0)
            throw new SessionExpiredException();
    }

    @Transactional
    public String rotateRefreshToken(RefreshToken oldToken) {
        this.revokeRefreshToken(oldToken);
        User user = oldToken.getUser();
        return this.issueRefreshToken(user);
    }

    public RefreshToken getValidToken(String rawToken) {
        String hashedToken = this.hashToken(rawToken);
        //TODO maybe change the way we throw here ??
        return refreshTokenRepository.findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(hashedToken, Instant.now())
                .orElseThrow(SessionExpiredException::new);
    }

}
