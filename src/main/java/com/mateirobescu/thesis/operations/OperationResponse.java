package com.mateirobescu.thesis.operations;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record OperationResponse (

    //TODO unsafe change later when auth comes in
    UUID id,
    UUID userId,
    UUID projectId,
    Long seq,
    Long clientSeq,
    Instant timestamp,
    Instant clientTimestamp,
    OperationType type,
    JsonNode payload
){
    public static OperationResponse fromOperation(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getUser().getId(),
                operation.getProject().getId(),
                operation.getSeq(),
                operation.getClientSeq(),
                operation.getTimestamp(),
                operation.getClientTimestamp(),
                operation.getType(),
                operation.getPayload()
        );
    }
}
