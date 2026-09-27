package com.mateirobescu.thesis.auth;

public record RefreshAccessTokenCommand(
        String refreshToken
) {
    public static RefreshAccessTokenCommand fromRequest(RefreshAccessTokensRequest request) {
        return new RefreshAccessTokenCommand(request.refreshToken());
    }
}
