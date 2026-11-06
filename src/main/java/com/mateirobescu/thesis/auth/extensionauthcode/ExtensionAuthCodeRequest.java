package com.mateirobescu.thesis.auth.extensionauthcode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExtensionAuthCodeRequest(
        @NotBlank
        //TODO maybe check this regex, it's ai gen
        @Pattern(regexp = "^[A-Za-z0-9\\-_]{43}$", message = "Invalid code challange")
        String codeChallenge,

        @NotBlank
        @Pattern(regexp = "^S256$", message = "Invalid code challenge method")
        String codeChallengeMethod
) {
}
