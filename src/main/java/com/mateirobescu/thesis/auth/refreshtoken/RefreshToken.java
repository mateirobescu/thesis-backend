package com.mateirobescu.thesis.auth.refreshtoken;

import com.mateirobescu.thesis.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, updatable = false, unique = true)
    private String tokenHash;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant revokedAt;

//    @NotNull
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
