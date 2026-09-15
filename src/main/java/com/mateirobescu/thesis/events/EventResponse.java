package com.mateirobescu.thesis.events;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record EventResponse (

    @JsonUnwrapped
    @NotNull
    EventResponseEnvelope envelope,

    @NotNull
    JsonNode payload
){
    public static EventResponse fromEvent(Event event) {
        return new EventResponse(new EventResponseEnvelope(
                event.getId(),
                event.getUser().getId(),
                event.getProject().getId(),
                event.getSeq(),
                event.getClientSeq(),
                event.getTimestamp(),
                event.getClientTimestamp(),
                event.getType()
            ),
            event.getPayload()
        );
    }
}
