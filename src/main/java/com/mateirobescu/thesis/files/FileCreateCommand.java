package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.auth.AuthUtils;

import java.util.UUID;

/**
 * Data needed to create a file in a project. The owner is the current user when built from a request.
 */
public record FileCreateCommand(
        UUID projectId,
        String path,
        UUID ownerId
) {
    public static FileCreateCommand fromRequest(UUID projectId, FileCreateRequest request) {
        return new FileCreateCommand(projectId, request.path(), AuthUtils.getCurrentUserId());
    }
}
