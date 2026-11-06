package com.mateirobescu.thesis.auth.token;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
