package com.mateirobescu.thesis.users;

public record UserPatchCommand(
        String email,
        String firstName,
        String lastName
) {
    public static UserPatchCommand fromRequest(UserPatchRequest request) {
        return new UserPatchCommand(request.email(), request.firstName(), request.lastName());
    }
}
