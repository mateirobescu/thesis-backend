package com.mateirobescu.thesis.users;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserResponse(
        UUID id
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(user.getId());
    }
}
