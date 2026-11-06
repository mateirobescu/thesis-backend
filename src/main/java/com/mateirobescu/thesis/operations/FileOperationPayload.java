package com.mateirobescu.thesis.operations;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileOperationPayload(
        @NotNull
        UUID fileId,

        @NotNull
        Long charOffset,

        @NotNull
        Long length,

        @NotNull
        String chars
) implements OperationPayload {
}
