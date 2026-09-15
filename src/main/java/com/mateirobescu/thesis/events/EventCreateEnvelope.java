package com.mateirobescu.thesis.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventCreateEnvelope(

    //TODO unsafe change later when auth comes in
    @NotNull
    UUID userId,

    @NotNull
    UUID projectId,

    @NotNull
    Long clientSeq,

    @NotNull
    Instant clientTimestamp,

    @NotNull
    EventType type
)
{}
