package com.mateirobescu.thesis.auth.refreshtoken;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank
        String refreshToken
) {
}
