package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.User;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse (
    UUID id,
    String name,
    Instant createdAt,
    UUID ownerId
) {
    public static WorkspaceResponse fromWorkspace(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getCreatedAt(),
                workspace.getOwner().getId()
        );
    }
}
