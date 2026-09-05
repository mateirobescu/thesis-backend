package com.mateirobescu.thesis.events;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EventCreateCommand(
        UUID fileId,
        UUID userId,
        Long clientSeq,
        Long clientProjectSeq,
        Instant clientTimestamp,
        Integer char_offset,
        Integer length,
        String chars
) {
    public static EventCreateCommand fromRequest(EventCreateRequest request) {
        return new EventCreateCommand(
                request.fileId(),
                request.userId(),
                request.clientSeq(),
                request.clientProjectSeq(),
                request.clientTimestamp(),
                request.char_offset(),
                request.length(),
                request.chars()
        );
    }
}
