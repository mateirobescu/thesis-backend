package com.mateirobescu.thesis.auth.refreshtoken;

import com.mateirobescu.thesis.auth.token.TokenPair;

public record RefreshResponse(
        TokenPair tokens
) {
    public static RefreshResponse fromTokenPair(TokenPair tokens) {
        return new RefreshResponse(tokens);
    }
}
