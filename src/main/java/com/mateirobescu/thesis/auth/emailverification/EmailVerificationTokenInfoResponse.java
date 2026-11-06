package com.mateirobescu.thesis.auth.emailverification;

import java.time.Instant;

public record EmailVerificationTokenInfoResponse(
        String email,
        Instant createdAt,
        Instant expiresAt
) {
    //TODO don t like the join with user here happens in the controller
    public static EmailVerificationTokenInfoResponse fromToken(EmailVerificationToken token) {
        return new EmailVerificationTokenInfoResponse(token.getUser().getEmail(), token.getCreatedAt(), token.getExpiresAt());
    }
}
