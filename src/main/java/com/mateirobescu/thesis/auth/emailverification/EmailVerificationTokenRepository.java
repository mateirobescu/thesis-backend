package com.mateirobescu.thesis.auth.emailverification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHashAndExpiresAtAfterAndUsedAtIsNull(String tokenHash, Instant expiresAtAfter);

    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.usedAt = :now WHERE e.id = :id AND e.usedAt IS NULL")
    int markUsedAt(@Param("id") UUID tokenId, @Param("now") Instant now);

    default boolean markUsed(UUID tokenId) {
        return this.markUsedAt(tokenId, Instant.now()) > 0;
    }

}
