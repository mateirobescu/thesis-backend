package com.mateirobescu.thesis.auth.signin;

import com.mateirobescu.thesis.auth.token.TokenPair;
import com.mateirobescu.thesis.users.UserResponse;

public record SignInResponse(
        TokenPair tokens,
        //TODO maybe the sign in should not also return the user
        UserResponse user
) {
    public static SignInResponse fromResult(SignInResult result) {
        return new SignInResponse(
                result.tokens(),
                UserResponse.fromUser(result.user())
        );
    }
}
