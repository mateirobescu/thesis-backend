package com.mateirobescu.thesis.auth.extensionauthcode;

import com.mateirobescu.thesis.auth.token.TokenUtils;
import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ExtensionAuthCodeService {

    private final ExtensionAuthCodeRepository extensionAuthCodeRepository;
    private final UserService userService;
    private final Long codeValiditySeconds;

    ExtensionAuthCodeService(
            ExtensionAuthCodeRepository extensionAuthCodeRepository,
            UserService userService,
            @Value("${auth.extension-auth-code.validity-seconds}")
            Long codeValiditySeconds
    ) {
        this.extensionAuthCodeRepository = extensionAuthCodeRepository;
        this.userService = userService;
        this.codeValiditySeconds = codeValiditySeconds;
    }

    public String issueAuthCode(ExtensionAuthCodeCommand command) {
        User user = userService.getUserById(command.userId());
        Instant expirationTimestamp = Instant.now().plus(this.codeValiditySeconds, ChronoUnit.SECONDS);

        String rawCode = TokenUtils.generateSecureRandomToken();
        String codeHash = TokenUtils.hashToken(rawCode);

        ExtensionAuthCode authCode = ExtensionAuthCode.builder()
                .codeHash(codeHash)
                .codeChallenge(command.codeChallenge())
                .codeChallengeMethod(command.codeChallengeMethod())
                .expiresAt(expirationTimestamp)
                .user(user)
                .build();

        extensionAuthCodeRepository.save(authCode);

        return rawCode;
    }

    private ExtensionAuthCode getValidAuthCode(String rawCode) {
        String codeHash = TokenUtils.hashToken(rawCode);
        return this.extensionAuthCodeRepository
                .findByCodeHashAndExpiresAtAfterAndUsedAtIsNull(codeHash, Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired token"));
    }

    public User consumeAuthCode(ExtensionSignInCommand command) {
        ExtensionAuthCode authCode = this.getValidAuthCode(command.authCode());

        String verifierHash = TokenUtils.hashToken(command.codeVerifier());

        if(!verifierHash.equals(authCode.getCodeChallenge()))
            throw new AuthenticationException("Invalid or expired token");

        //TODO race condition if the same code is used twice at the same time
        boolean marked = this.extensionAuthCodeRepository.markUsed(authCode.getId());
        if(!marked)
            throw new AuthenticationException("Invalid or expired token");

        return authCode.getUser();
    }

}
