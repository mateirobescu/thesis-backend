package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.validation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileCreateRequest (
        @NotBlank
        @ValidPath
        String path
) {
}
