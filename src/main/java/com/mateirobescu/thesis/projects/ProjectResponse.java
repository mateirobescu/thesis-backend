package com.mateirobescu.thesis.projects;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String path,
        UUID ownerId,
        Instant createdAt
) {
    public static ProjectResponse fromProject(Project project) {
        return new ProjectResponse(
                project.getId(),
                //TODO does this trigger a join?
                project.getWorkspace().getId(),
                project.getName(),
                project.getPath(),
                project.getOwner().getId(),
                project.getCreatedAt()
        );
    }
}
