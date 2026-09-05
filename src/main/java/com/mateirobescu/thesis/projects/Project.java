package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.files.File;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.workspaces.Workspace;
import com.mateirobescu.thesis.workspaces.WorkspacePatch;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.jdbc.Work;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(
    name = "projects",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_projects_workspace_path_name",
            columnNames = {"workspace_id", "path", "name"}
        )
    }
)
public class Project {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @NotBlank
    @Builder.Default
    @Column(nullable = false)
    private String path = "/";

    //TODO exclude this from the builder
    //TODO maybe change in the future?
    @NotNull
    @Builder.Default
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long seq = 0L;

    public Project incrementSeq() {
        this.seq++;
        return this;
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Project applyPatch(ProjectPatch patch) {
        if(patch.name() != null) this.name = patch.name();
        if(patch.path() != null) this.path = patch.path();
        if(patch.workspace() != null) this.workspace = patch.workspace();
        if(patch.owner() != null) this.owner = patch.owner();
        return this;
    }

}
