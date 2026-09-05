package com.mateirobescu.thesis.events;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID file,
        UUID user,
        Long seq,
        Long projectSeq,
        Long clientSeq,
        Long clientProjectSeq,
        Instant serverTimestamp,
        Instant clientTimestamp,
        Integer offset,
        Integer length,
        String chars
) {
    public static EventResponse fromEvent(Event event) {
        return new EventResponse(
                event.getId(),
                event.getFile().getId(),
                event.getUser().getId(),
                event.getSeq(),
                event.getProjectSeq(),
                event.getClientSeq(),
                event.getClientProjectSeq(),
                event.getServerTimestamp(),
                event.getClientTimestamp(),
                event.getChar_offset(),
                event.getLength(),
                event.getChars()
        );
    }
}
