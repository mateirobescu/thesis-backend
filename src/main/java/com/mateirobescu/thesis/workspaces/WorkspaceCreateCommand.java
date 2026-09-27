package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.auth.AuthUtils;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public record WorkspaceCreateCommand(
        String name,
        UUID ownerId
) {
    public static WorkspaceCreateCommand fromRequest(WorkspaceCreateRequest request) {
        return new WorkspaceCreateCommand(request.name(), AuthUtils.getCurrentUserId());
    }
}
