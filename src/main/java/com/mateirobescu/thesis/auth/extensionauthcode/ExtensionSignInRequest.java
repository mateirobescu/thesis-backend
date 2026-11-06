package com.mateirobescu.thesis.auth.extensionauthcode;

import jakarta.validation.constraints.NotBlank;

public record ExtensionSignInRequest(
        @NotBlank
        String authCode,

        @NotBlank
        String codeVerifier
) {
}
