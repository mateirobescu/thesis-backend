package com.mateirobescu.thesis.workspaces;

import java.util.UUID;

/**
 * Fields to change on a workspace. A {@code null} field means "leave unchanged"; a present name is trimmed.
 */
public record WorkspacePatchCommand(
        String name,
        UUID ownerId
) {

    public WorkspacePatchCommand {
        name = name == null ? null : name.trim();
    }

    public static WorkspacePatchCommand fromRequest(WorkspacePatchRequest request) {
        return new WorkspacePatchCommand(request.name(), request.ownerId());
    }
}
