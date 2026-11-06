package com.mateirobescu.thesis.auth.emailverification;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank
        String token
) {
}
