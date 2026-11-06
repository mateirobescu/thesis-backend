package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
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
 * A file inside a project, identified there by its path. A path is unique per project among files that are not
 * soft-deleted; this is the partial unique index {@code uk_files_project_path_active}, which cannot be expressed
 * in JPA and is defined in the Flyway migration. The file is soft-deleted through {@code deletedAt}, which
 * {@link #restoreFile()} clears again, and {@code version} is used for optimistic locking.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(name = "files")
public class File {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String path;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Instant deletedAt;

    @Version
    private Long version;

    public File markDeleted() {
        this.deletedAt = Instant.now();
        return this;
    }

    public File restoreFile() {
        this.deletedAt = null;
        return this;
    }

    public File applyPatch(FilePatch patch) {
        if(patch.path() != null) this.path = patch.path();
        if(patch.owner() != null) this.owner = patch.owner();
        return this;
    }

}
