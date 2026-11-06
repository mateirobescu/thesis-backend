package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.auth.emailverification.EmailVerificationCommand;
import com.mateirobescu.thesis.auth.emailverification.EmailVerificationService;
import com.mateirobescu.thesis.auth.emailverification.EmailVerificationToken;
import com.mateirobescu.thesis.auth.extensionauthcode.ExtensionAuthCodeCommand;
import com.mateirobescu.thesis.auth.extensionauthcode.ExtensionAuthCodeService;
import com.mateirobescu.thesis.auth.extensionauthcode.ExtensionSignInCommand;
import com.mateirobescu.thesis.auth.jwt.JwtService;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshCommand;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshToken;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshTokenService;
import com.mateirobescu.thesis.auth.signin.EmailSignInCommand;
import com.mateirobescu.thesis.auth.signin.SignInResult;
import com.mateirobescu.thesis.auth.signup.SignUpCommand;
import com.mateirobescu.thesis.auth.token.TokenPair;
import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserCreateCommand;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final ExtensionAuthCodeService extensionAuthCodeService;

    private final String dummyHash;

    public AuthService(PasswordEncoder passwordEncoder, UserService userService, RefreshTokenService refreshTokenService, JwtService jwtService, EmailVerificationService emailVerificationService, ExtensionAuthCodeService extensionAuthCodeService) {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.extensionAuthCodeService = extensionAuthCodeService;

        this.dummyHash = this.encodePassword("dummy-password-" + UUID.randomUUID());
    }

    private String encodePassword(String rawPassword) {
        return this.passwordEncoder.encode(rawPassword);
    }

    public User signUp(SignUpCommand command) {
        String passwordHash = this.encodePassword(command.rawPassword());

        UserCreateCommand userCreateCommand = UserCreateCommand.builder()
                .email(command.email())
                .passwordHash(passwordHash)
                .firstName(command.firstName())
                .lastName(command.lastName())
                .build();

        User user = userService.createUser(userCreateCommand);
        emailVerificationService.issueEmailVerificationToken(user);

        return user;
    }

    private SignInResult signIn(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return SignInResult.builder()
                .tokens(new TokenPair(accessToken, refreshToken))
                .user(user)
                .build();
    }

    public SignInResult signInWithEmail(EmailSignInCommand command) {
        User user = userService.findUserByEmail(command.email()).orElse(null);
        // Calculating the hash no matter what, because an attacker can guess if the account
        // exists based on the fact that requests that are hashing take considerably much more time
        String correctPasswordHash = user != null ? user.getPasswordHash() : this.dummyHash;
        boolean passwordMatches = passwordEncoder.matches(command.rawPassword(), correctPasswordHash);

        if (user == null || !passwordMatches)
            throw new AuthenticationException("Invalid email or password");

        if (!user.isEmailVerified())
            throw new AuthenticationException("Please verify your email before signing in");

        return this.signIn(user);
    }

    @Transactional
    public SignInResult signInWithExtensionAuthCode(ExtensionSignInCommand command) {
        User user = this.extensionAuthCodeService.consumeAuthCode(command);
        return this.signIn(user);
    }

    public TokenPair refresh(RefreshCommand command) {
        RefreshToken refreshToken = this.refreshTokenService.getValidRefreshToken(command.refreshToken());
        String newAccessToken = this.jwtService.generateAccessToken(refreshToken.getUser().getId());
        String newRefreshToken = this.refreshTokenService.rotateRefreshToken(refreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void verifyEmail(EmailVerificationCommand command) {
        User user = emailVerificationService.consumeEmailVerificationToken(command.token());
        userService.verifyUserEmail(user);
    }

    public EmailVerificationToken getEmailVerificationTokenInfo(EmailVerificationCommand command) {
        return emailVerificationService.getValidEmailVerificationToken(command.token());
    }

    public String issueExtensionAuthCode(ExtensionAuthCodeCommand command) {
        return this.extensionAuthCodeService.issueAuthCode(command);
    }

}
