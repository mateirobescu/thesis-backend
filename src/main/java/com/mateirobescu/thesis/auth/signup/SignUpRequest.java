package com.mateirobescu.thesis.auth.signup;

import com.mateirobescu.thesis.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@FieldMatch(first = "rawPassword", second = "rawPasswordConfirm", message = "Password confirmation does not match")
public record SignUpRequest(
        @NotBlank
        @Email
        String email,

        //TODO add password validation
        @NotBlank
        String rawPassword,

        @NotBlank
        String rawPasswordConfirm,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName
) {
}
