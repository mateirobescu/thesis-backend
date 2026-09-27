package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.users.UserResponse;

public record LoginResponse(
        AccessTokens tokens,
        UserResponse user
) {
    public static LoginResponse fromResult(LoginResult result) {
        return new LoginResponse(
                result.tokens(),
                UserResponse.fromUser(result.user())
        );
    }
}
