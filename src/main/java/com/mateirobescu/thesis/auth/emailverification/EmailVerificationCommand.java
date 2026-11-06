package com.mateirobescu.thesis.auth.emailverification;

public record EmailVerificationCommand(
        String token
) {
    public static EmailVerificationCommand fromRequest(EmailVerificationRequest request) {
        return new EmailVerificationCommand(request.token());
    }
}
