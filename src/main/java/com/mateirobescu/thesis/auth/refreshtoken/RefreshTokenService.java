package com.mateirobescu.thesis.auth.refreshtoken;

import com.mateirobescu.thesis.auth.token.TokenUtils;
import com.mateirobescu.thesis.exception.SessionExpiredException;
import com.mateirobescu.thesis.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Long tokenValidityDays;

    public RefreshTokenService(
            @Value("${auth.refresh-token.validity-days:30}") Long tokenValidityDays,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.tokenValidityDays = tokenValidityDays;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issueRefreshToken(User user) {
        String rawToken = TokenUtils.generateSecureRandomToken();
        String tokenHash = TokenUtils.hashToken(rawToken);
        Instant expirationTimestamp = Instant.now().plus(tokenValidityDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expirationTimestamp)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
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

    public RefreshToken getValidRefreshToken(String rawToken) {
        String tokenHash = TokenUtils.hashToken(rawToken);
        //TODO maybe change the way we throw here ??
        return refreshTokenRepository.findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(tokenHash, Instant.now())
                .orElseThrow(SessionExpiredException::new);
    }

}
