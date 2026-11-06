package com.mateirobescu.thesis.auth;

import com.mateirobescu.thesis.auth.emailverification.*;
import com.mateirobescu.thesis.auth.extensionauthcode.*;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshCommand;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshRequest;
import com.mateirobescu.thesis.auth.refreshtoken.RefreshResponse;
import com.mateirobescu.thesis.auth.signin.EmailSignInCommand;
import com.mateirobescu.thesis.auth.signin.EmailSignInRequest;
import com.mateirobescu.thesis.auth.signin.SignInResponse;
import com.mateirobescu.thesis.auth.signup.SignUpCommand;
import com.mateirobescu.thesis.auth.signup.SignUpRequest;
import com.mateirobescu.thesis.users.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return UserResponse.fromUser(
                this.authService.signUp(SignUpCommand.fromRequest(request))
        );
    }

    @PostMapping("/email-sign-in")
    public SignInResponse signInWithEmail(@Valid @RequestBody EmailSignInRequest request) {
        return SignInResponse.fromResult(
                authService.signInWithEmail(EmailSignInCommand.fromRequest(request))
        );
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return RefreshResponse.fromTokenPair(
                this.authService.refresh(RefreshCommand.fromRequest(request))
        );
    }

    @PostMapping("/verify-email")
    public EmailVerificationResponse verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.verifyEmail(EmailVerificationCommand.fromRequest(request));
        return EmailVerificationResponse.successful();
    }

    @GetMapping("/verify-email/info")
    public EmailVerificationTokenInfoResponse getEmailVerificationTokenInfo(@RequestParam String token) {
        return EmailVerificationTokenInfoResponse.fromToken(
                authService.getEmailVerificationTokenInfo(new EmailVerificationCommand(token))
        );
    }

    @PostMapping("/extension-auth-code")
    public ExtensionAuthCodeResponse issueExtensionAuthCode(@Valid @RequestBody ExtensionAuthCodeRequest request) {
        return ExtensionAuthCodeResponse.fromAuthCode(
                this.authService.issueExtensionAuthCode(ExtensionAuthCodeCommand.fromRequest(request))
        );
    }

    @PostMapping("/extension-sign-in")
    public SignInResponse signInWithExtensionAuthCode(@Valid @RequestBody ExtensionSignInRequest request) {
        return SignInResponse.fromResult(
                this.authService.signInWithExtensionAuthCode(ExtensionSignInCommand.fromRequest(request))
        );
    }

}
