package com.mateirobescu.thesis.auth.extensionauthcode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface ExtensionAuthCodeRepository extends JpaRepository<ExtensionAuthCode, UUID> {
    Optional<ExtensionAuthCode> findByCodeHashAndExpiresAtAfterAndUsedAtIsNull(String codeHash, Instant expiresAtAfter);

    //TODO maybe clear and flush automatically ?
    @Modifying
    @Query("UPDATE ExtensionAuthCode e SET e.usedAt = :now WHERE e.id = :id AND e.usedAt IS NULL")
    int markUsedAt(@Param("id") UUID codeId, @Param("now") Instant now);

    default boolean markUsed(UUID codeId) {
        return this.markUsedAt(codeId, Instant.now()) > 0;
    }
}
