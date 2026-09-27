package com.mateirobescu.thesis.users;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
