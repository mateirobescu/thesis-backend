package com.mateirobescu.thesis.events;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;

public record FileEventCreateRequest (

    @JsonUnwrapped
    @NotNull
    EventCreateEnvelope envelope,

    @NotNull
    FileEventPayload payload
) { }
