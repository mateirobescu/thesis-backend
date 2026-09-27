package com.mateirobescu.thesis.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshAccessTokensRequest(
        @NotBlank
        String refreshToken
) {
}
