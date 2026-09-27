package com.mateirobescu.thesis.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String rawPassword,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName
) {
}
