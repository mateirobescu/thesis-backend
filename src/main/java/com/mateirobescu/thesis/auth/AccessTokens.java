package com.mateirobescu.thesis.auth;

public record AccessTokens(
        String accessToken,
        String refreshToken
) {
}
