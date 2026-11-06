package com.mateirobescu.thesis.auth.signin;

import com.mateirobescu.thesis.auth.token.TokenPair;
import com.mateirobescu.thesis.users.User;
import lombok.Builder;

@Builder
public record SignInResult(
        TokenPair tokens,
        User user
) {
}
