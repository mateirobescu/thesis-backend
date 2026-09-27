package com.mateirobescu.thesis.auth.refreshtoken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndExpiresAtAfterAndRevokedAtIsNull(String tokenHash, Instant expiresAtAfter);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.id = :id AND t.revokedAt IS NULL")
    int revokeIfNotAlreadyRevoked(@Param("id") UUID id, @Param("now") Instant now);
}
