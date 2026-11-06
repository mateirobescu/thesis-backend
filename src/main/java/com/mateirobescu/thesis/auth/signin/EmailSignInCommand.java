package com.mateirobescu.thesis.auth.signin;

public record EmailSignInCommand(
        String email,
        String rawPassword
) {
    public static EmailSignInCommand fromRequest(EmailSignInRequest request) {
        return new EmailSignInCommand(request.email(), request.rawPassword());
    }
}
