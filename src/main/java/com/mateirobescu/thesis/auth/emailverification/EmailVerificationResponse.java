package com.mateirobescu.thesis.auth.emailverification;

public record EmailVerificationResponse(
        boolean verified
) {
    public static EmailVerificationResponse successful() {
        return new EmailVerificationResponse(true);
    }

    public static EmailVerificationResponse failed() {
        return new EmailVerificationResponse(false);
    }
}
