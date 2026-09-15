package com.mateirobescu.thesis.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventResponseEnvelope(

        //TODO unsafe change later when auth comes in
        UUID id,
        UUID userId,
        UUID projectId,
        Long seq,
        Long clientSeq,
        Instant timestamp,
        Instant clientTimestamp,
        EventType type
)
{}