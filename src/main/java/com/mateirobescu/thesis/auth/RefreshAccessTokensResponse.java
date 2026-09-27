package com.mateirobescu.thesis.auth;

public record RefreshAccessTokensResponse(
        AccessTokens tokens
) {
    public static RefreshAccessTokensResponse fromAccessTokens(AccessTokens tokens) {
        return new RefreshAccessTokensResponse(tokens);
    }
}
