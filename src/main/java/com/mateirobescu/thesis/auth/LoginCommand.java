package com.mateirobescu.thesis.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        String email,
        String rawPassword
) {
    public static LoginCommand fromRequest(LoginRequest request) {
        return new LoginCommand(request.email(), request.rawPassword());
    }
}
