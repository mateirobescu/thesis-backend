package com.mateirobescu.thesis.auth.signup;

public record SignUpCommand(
        String email,
        String rawPassword,
        String firstName,
        String lastName
) {
    public static SignUpCommand fromRequest(SignUpRequest request) {
        return new SignUpCommand(request.email(), request.rawPassword(), request.firstName(), request.lastName());
    }
}
