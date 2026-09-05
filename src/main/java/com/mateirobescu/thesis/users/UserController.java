package com.mateirobescu.thesis.users;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
class UserController {

    UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse postCreateUser(@Valid @RequestBody UserCreateRequest request) {
        return UserResponse.fromUser(userService.createUser());
    }

    @GetMapping
    public UserListResponse getAllUsers() {
        return UserListResponse.fromUsers(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        return UserResponse.fromUser(user);
    }

    @PatchMapping("/{userId}")
    public UserResponse patchUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserPatchRequest request
    ) {
        //TODO don't like the request arriving into the service
        return UserResponse.fromUser(userService.patchUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }

}
