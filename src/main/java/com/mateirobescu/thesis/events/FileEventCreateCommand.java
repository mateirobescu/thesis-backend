package com.mateirobescu.thesis.events;


public record FileEventCreateCommand(
        EventCreateEnvelope envelope,
        FileEventPayload payload
) {
    public static FileEventCreateCommand fromRequest(FileEventCreateRequest request) {
        return new FileEventCreateCommand(request.envelope(), request.payload());
    }
}
