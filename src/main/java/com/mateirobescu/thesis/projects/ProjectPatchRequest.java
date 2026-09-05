package com.mateirobescu.thesis.projects;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.workspaces.Workspace;

import java.util.UUID;

public record ProjectPatchRequest (
        UUID workspaceId,
        String name,
        String path,
        UUID ownerId
) {}