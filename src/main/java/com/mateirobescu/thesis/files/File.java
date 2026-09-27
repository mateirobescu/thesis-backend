package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectPatch;
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
/*
TODO will have to use this
DROP INDEX IF EXISTS uk_files_project_path; -- remove whatever ddl-auto generated before
CREATE UNIQUE INDEX uk_files_project_path_active
ON files (project_id, path)
WHERE deleted_at IS NULL;
 */
@Table(
    name = "files",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_files_project_path",
            columnNames = {"project_id", "path", "deleted_at"}
        )
    }
)
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

    //TODO exclude this from the builder
    //TODO maybe change in the future?
    @NotNull
    @Builder.Default
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long seq = 0L;

    public File incrementSeq() {
        this.seq++;
        return this;
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Instant deletedAt;

    public File markAsDeleted() {
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
