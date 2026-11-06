package com.mateirobescu.thesis.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
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

/**
 * A user account. It is soft-deleted through {@code deletedAt}, counts as verified once {@code emailVerifiedAt}
 * is set, and uses {@code version} for optimistic locking. Email uniqueness among users that are not soft-deleted is the partial
 * unique index {@code uk_users_email_active}, defined in the Flyway migration.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(name = "users")
public class User {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    private Instant deletedAt;

    //TODO some recurring tasks to disable old unactivated accounts
    private Instant emailVerifiedAt;

    @Version
    private Long version;

    public User markDeleted() {
        this.deletedAt = Instant.now();
        return this;
    }

    public User unmarkDeleted() {
        this.deletedAt = null;
        return this;
    }

    public User markEmailVerified() {
        this.emailVerifiedAt = Instant.now();
        return this;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public User patch(UserPatchCommand command) {
        if(command.email() != null) this.email = command.email();
        if(command.firstName() != null) this.firstName = command.firstName();
        if(command.lastName() != null) this.lastName = command.lastName();

        return this;
    }

}
