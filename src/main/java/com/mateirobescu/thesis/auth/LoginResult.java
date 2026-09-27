package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.users.User;
import lombok.Builder;
import org.apache.logging.log4j.util.Strings;

@Builder
public record LoginResult(
        AccessTokens tokens,
        User user
) {
}
