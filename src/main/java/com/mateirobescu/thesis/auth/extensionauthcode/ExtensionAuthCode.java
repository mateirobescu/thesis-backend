package com.mateirobescu.thesis.auth.extensionauthcode;

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
@Table(name = "extension_auth_codes")
public class ExtensionAuthCode {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, updatable = false, unique = true)
    private String codeHash;

    @NotBlank
    @Column(nullable = false, updatable = false, unique = true)
    private String codeChallenge;

    //TODO should probably check this to be S256 here too
    @NotBlank
    @Column(nullable = false, updatable = false)
    private String codeChallengeMethod = "S256";

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant usedAt;

    //    @NotNull
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
