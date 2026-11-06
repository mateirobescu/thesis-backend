package com.mateirobescu.thesis.auth.refreshtoken;

public record RefreshCommand(
        String refreshToken
) {
    public static RefreshCommand fromRequest(RefreshRequest request) {
        return new RefreshCommand(request.refreshToken());
    }
}
