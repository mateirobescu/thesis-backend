package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.auth.AuthUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Data needed to create a project. The path may be {@code null}, in which case the project is created at
 * {@code "/"}.
 */
public record ProjectCreateCommand (
        UUID workspaceId,
        String name,
        String path,
        UUID ownerId
) {
    public static ProjectCreateCommand fromRequest(ProjectCreateRequest request) {
        return new ProjectCreateCommand(
                request.workspaceId(),
                request.name(),
                request.path(),
                AuthUtils.getCurrentUserId()
        );
    }
}