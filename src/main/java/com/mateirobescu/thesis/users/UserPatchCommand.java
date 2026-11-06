package com.mateirobescu.thesis.users;

import java.util.Locale;

/**
 * Fields to change on a user. A {@code null} field means "leave unchanged"; present values are normalized the same
 * way as in {@link UserCreateCommand}.
 */
public record UserPatchCommand(
        String email,
        String firstName,
        String lastName
) {
    public UserPatchCommand {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        firstName = firstName == null ? null : firstName.trim();
        lastName = lastName == null ? null : lastName.trim();
    }

    public static UserPatchCommand fromRequest(UserPatchRequest request) {
        return new UserPatchCommand(request.email(), request.firstName(), request.lastName());
    }
}
