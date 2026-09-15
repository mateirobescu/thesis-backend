package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileResolveRequest(
        @NotNull
        @NotBlank
        @ValidPath
        String fullPath,

        @NotNull
        UUID userId  //TODO should probably delete for security reasons
) {
}
