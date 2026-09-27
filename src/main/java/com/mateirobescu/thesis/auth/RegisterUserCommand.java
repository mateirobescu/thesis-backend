package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.users.UserCreateCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserCommand(
        String email,
        String rawPassword,
        String firstName,
        String lastName
) {
        public static RegisterUserCommand fromRequest(RegisterUserRequest request) {
                return new RegisterUserCommand(request.email(), request.rawPassword(), request.firstName(), request.lastName());
        }
}
