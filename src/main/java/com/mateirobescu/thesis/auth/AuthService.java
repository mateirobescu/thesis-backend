package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.auth.jwt.JwtService;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshToken;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshTokenService;
import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserCreateCommand;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthService(PasswordEncoder passwordEncoder, UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    public String encodePassword(String rawPassword) {
        return this.passwordEncoder.encode(rawPassword);
    }

    public User registerUser(RegisterUserCommand command) {
        String hashedPassword = this.passwordEncoder.encode(command.rawPassword());

        UserCreateCommand userCreateCommand = UserCreateCommand.builder()
                .email(command.email())
                .hashedPassword(hashedPassword)
                .firstName(command.firstName())
                .lastName(command.lastName())
                .build();

        return userService.createUser(userCreateCommand);
    }

    public LoginResult loginUser(LoginCommand command) {
        User user = userService.getUserByEmail(command.email());
        if(!passwordEncoder.matches(command.rawPassword(), user.getPasswordHash()))
            throw new AuthenticationException("Invalid email or password");

        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return LoginResult.builder()
                .tokens(new AccessTokens(accessToken, refreshToken))
                .user(user)
                .build();
    }

    public AccessTokens refreshAccessTokens(RefreshAccessTokenCommand command) {
        RefreshToken refreshToken = this.refreshTokenService.getValidToken(command.refreshToken());
        String newAccessToken = this.jwtService.generateAccessToken(refreshToken.getUser().getId());
        String newRefreshToken = this.refreshTokenService.rotateRefreshToken(refreshToken);

        return new AccessTokens(newAccessToken, newRefreshToken);
    }
}
