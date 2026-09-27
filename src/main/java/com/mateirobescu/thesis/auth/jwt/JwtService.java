package com.mateirobescu.thesis.auth.jwt;

import com.mateirobescu.thesis.auth.refreshtoken.RefreshToken;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshTokenService;
import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.exception.NotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTokenValidityMinutes;
    private final RefreshTokenService refreshTokenService;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-minutes:15}") long accessTokenValidityMinutes,
            RefreshTokenService refreshTokenService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityMinutes = accessTokenValidityMinutes;
        this.refreshTokenService = refreshTokenService;
    }

    public String generateAccessToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidityMinutes, ChronoUnit.MINUTES)))
                .signWith(secretKey)
                .compact();
    }

    public UUID validateAndExtractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    public String refreshAccessToken(String rawRefreshToken) {
        RefreshToken token = this.refreshTokenService.getValidToken(rawRefreshToken);
        return this.generateAccessToken(token.getUser().getId());
    }
}
