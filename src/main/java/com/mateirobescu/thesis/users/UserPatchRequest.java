package com.mateirobescu.thesis.users;

import jakarta.validation.constraints.Email;

public record UserPatchRequest(
        @Email
        String email,
        String firstName,
        String lastName
) {
}
