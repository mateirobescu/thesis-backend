package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserListResponse;
import com.mateirobescu.thesis.users.UserResponse;

import java.util.Arrays;
import java.util.List;

public record EventListResponse (
        List<EventResponse> events
) {
    public static EventListResponse fromEvents(Event... event) {
        return EventListResponse.fromEvents(Arrays.stream(event).toList());
    }

    public static EventListResponse fromEvents(List<Event> events) {
        return new EventListResponse(events.stream()
                .map(EventResponse::fromEvent)
                .toList()
        );
    }
}