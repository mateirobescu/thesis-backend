package com.mateirobescu.thesis.users;

import lombok.Builder;

import java.util.Locale;

/**
 * Data needed to create a user. The constructor normalizes it: the email is trimmed and lower-cased, and the
 * names are trimmed.
 */
@Builder
public record UserCreateCommand (
        String email,
        String passwordHash,
        String firstName,
        String lastName
) {

    public UserCreateCommand {
        email = email.trim().toLowerCase(Locale.ROOT);
        firstName = firstName.trim();
        lastName = lastName.trim();
    }

}
