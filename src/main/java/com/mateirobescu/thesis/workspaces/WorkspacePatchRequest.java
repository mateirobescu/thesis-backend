package com.mateirobescu.thesis.workspaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WorkspacePatchRequest(
        String name,
        UUID ownerId
) {
}