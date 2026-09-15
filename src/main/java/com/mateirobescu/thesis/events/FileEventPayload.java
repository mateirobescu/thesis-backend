package com.mateirobescu.thesis.events;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FileEventPayload(
        @NotNull
        UUID fileId,

        @NotNull
        Long charOffset,

        @NotNull
        Long length,

        @NotNull
        String chars
) {
}
