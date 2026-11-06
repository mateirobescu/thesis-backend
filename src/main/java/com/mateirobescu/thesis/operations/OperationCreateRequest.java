package com.mateirobescu.thesis.operations;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Body of {@code POST /projects/{projectId}/operations}. The payload is kept as raw JSON because its structure
 * depends on {@code type}.
 */
public record OperationCreateRequest(

        @NotNull
        Long clientSeq,

        @NotNull
        Instant clientTimestamp,

        @NotNull
        OperationType type,

        @NotNull
        JsonNode payload
) { }
