package com.mateirobescu.thesis.projects;

import java.util.UUID;

/**
 * Fields to change on a project. A {@code null} field means "leave unchanged".
 */
public record ProjectPatchCommand(
        UUID workspaceId,
        String name,
        String path,
        UUID ownerId
) {
    public static ProjectPatchCommand fromRequest(ProjectPatchRequest request) {
        return new ProjectPatchCommand(request.workspaceId(), request.name(), request.path(), request.ownerId());
    }
}
