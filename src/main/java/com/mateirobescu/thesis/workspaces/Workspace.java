package com.mateirobescu.thesis.workspaces;

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

/**
 * A workspace that holds projects and belongs to one owner. It is soft-deleted through {@code deletedAt}, and
 * {@code version} is used for optimistic locking.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(name = "workspaces")
public class Workspace {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    //TODO maybe names should be unique somehow?
    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deletedAt;

    @Version
    private Long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Workspace applyPatch(WorkspacePatch patch) {
        if(patch.name() != null) this.name = patch.name();
        if(patch.owner() != null) this.owner = patch.owner();
        return this;
    }

    public Workspace markDeleted() {
        this.deletedAt = Instant.now();
        return this;
    }

}
