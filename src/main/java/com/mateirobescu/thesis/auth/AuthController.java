package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.users.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse registerUser(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.fromUser(
                this.authService.registerUser(RegisterUserCommand.fromRequest(request))
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.fromResult(authService.loginUser(LoginCommand.fromRequest(request)));
    }

    @PostMapping("/refresh")
    public RefreshAccessTokensResponse refreshToken(@Valid @RequestBody RefreshAccessTokensRequest request) {
        return RefreshAccessTokensResponse.fromAccessTokens(this.authService.refreshAccessTokens(RefreshAccessTokenCommand.fromRequest(request)));
    }
}
