package com.mateirobescu.thesis.files;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileCreateRequest (
        @NotNull
        UUID projectId,
        @NotNull
        @NotBlank
        String filename,
        String path,
        @NotNull
        UUID ownerId
) {
}
