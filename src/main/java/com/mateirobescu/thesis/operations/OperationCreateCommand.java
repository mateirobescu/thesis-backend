package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.auth.AuthUtils;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Data needed to record an operation. The user is the current user when built from a request.
 */
public record OperationCreateCommand(
        UUID userId,
        UUID projectId,
        Long clientSeq,
        Instant clientTimestamp,
        OperationType type,
        JsonNode payload
) {
    public static OperationCreateCommand fromRequest(UUID projectId, OperationCreateRequest request) {
        return new OperationCreateCommand(
                AuthUtils.getCurrentUserId(),
                projectId,
                request.clientSeq(),
                request.clientTimestamp(),
                request.type(),
                request.payload()
        );
    }
}
