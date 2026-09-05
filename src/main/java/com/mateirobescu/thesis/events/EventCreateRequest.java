package com.mateirobescu.thesis.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventCreateRequest (
        @NotNull
        UUID fileId,

        @NotNull
        UUID userId, //TODO unsafe change later when auth comes in

        @NotNull
        Long clientSeq,

        @NotNull
        Long clientProjectSeq,

        @NotNull
        Instant clientTimestamp,

        @NotNull
        Integer char_offset,

        @NotNull
        Integer length,

        @NotNull
        String chars
)
{ }
