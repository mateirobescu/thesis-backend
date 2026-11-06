package com.mateirobescu.thesis.auth.signin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSignInRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String rawPassword
) { }
