package com.mateirobescu.thesis.events;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
class EventController {

    EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/files")
    public EventResponse createFileEvent(@Valid @RequestBody FileEventCreateRequest request) {

        Event event = eventService.createFileEvent(
                FileEventCreateCommand.fromRequest(request)
        );

        return EventResponse.fromEvent(
            event
        );
    }

    @GetMapping
    public EventListResponse getEventsWithSeqGreaterThan(
            @RequestParam(value = "projectId") UUID projectId,
            @RequestParam("seqGreaterThan") Long seq
    ) {
        return EventListResponse.fromEvents(
                eventService.getProjectEventsWithSeqGreaterThan(projectId, seq)
        );
    }
}
