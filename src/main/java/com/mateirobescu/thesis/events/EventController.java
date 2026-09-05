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

    @PostMapping
    public EventResponse createEvent(@Valid @RequestBody EventCreateRequest request) {
        return EventResponse.fromEvent(
                eventService.createEvent(
                        EventCreateCommand.fromRequest(request)
                )
        );
    }

    //TODO might have to make responses more compact (too much useless data)
    @GetMapping
    public EventListResponse geFiletEventsWithSeqGreaterThan(
            @RequestParam("fileId") UUID fileId,
            @RequestParam("seqGreaterThan") Long seq
    ){
        return EventListResponse.fromEvents(
                eventService.getFileEventsWithSeqGreaterThan(fileId, seq)
        );
    }

}
