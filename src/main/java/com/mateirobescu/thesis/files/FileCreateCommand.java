package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.auth.AuthUtils;
import com.mateirobescu.thesis.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileCreateCommand(
        UUID projectId,
        String path,
        UUID ownerId
) {
    public static FileCreateCommand fromRequest(UUID projectId, FileCreateRequest request) {
        return new FileCreateCommand(projectId, request.path(), AuthUtils.getCurrentUserId());
    }
}
