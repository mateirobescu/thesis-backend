package com.mateirobescu.thesis.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProjectCreateRequest (
        @NotNull
        UUID workspaceId,
        @NotNull
        @NotBlank
        String name,
        String path,
        @NotNull
        UUID ownerId
) {
}
