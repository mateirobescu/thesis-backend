package com.mateirobescu.thesis.users;

import java.util.List;

public record UserListResponse(List<UserResponse> users) {
    public static UserListResponse fromUsers(List<User> users) {
        return new UserListResponse(users.stream()
                .map(UserResponse::fromUser)
                .toList()
        );
    }
}
