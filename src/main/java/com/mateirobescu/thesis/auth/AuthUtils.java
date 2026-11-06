package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.exception.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class AuthUtils {

    private AuthUtils() {}

    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || authentication.getName() == null)
            throw new AuthenticationException("No authenticated user found");
        return UUID.fromString(authentication.getName());
    }

}
