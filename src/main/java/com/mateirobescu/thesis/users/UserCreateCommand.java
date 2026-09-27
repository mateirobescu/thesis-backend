package com.mateirobescu.thesis.users;

import lombok.Builder;

@Builder
public record UserCreateCommand (
        String email,
        String hashedPassword,
        String firstName,
        String lastName
) {
}
