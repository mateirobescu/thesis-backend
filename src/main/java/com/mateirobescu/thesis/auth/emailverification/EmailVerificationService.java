package com.mateirobescu.thesis.auth.emailverification;

import com.mateirobescu.thesis.auth.token.TokenUtils;
import com.mateirobescu.thesis.email.EmailService;
import com.mateirobescu.thesis.exception.AuthenticationException;
import com.mateirobescu.thesis.users.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class EmailVerificationService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final Long tokenValidityHours;
    private final EmailService emailService;
    private final String frontendUrl;
    private final String verifyEmailPath;


    EmailVerificationService(
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            @Value("${auth.email-verification-token.validity-hours}")
            Long tokenValidityHours,
            EmailService emailService,
            @Value("${app.frontend.url}")
            String frontendUrl,
            @Value("${app.frontend.verify-email-path}")
            String verifyEmailPath
    ) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.tokenValidityHours = tokenValidityHours;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.verifyEmailPath = verifyEmailPath;
    }

    public void issueEmailVerificationToken(User user) {
        String rawToken = TokenUtils.generateSecureRandomToken();
        String tokenHash = TokenUtils.hashToken(rawToken);
        Instant expirationTimestamp = Instant.now().plus(tokenValidityHours, ChronoUnit.HOURS);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expirationTimestamp)
                .build();

        emailVerificationTokenRepository.save(token);

        this.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
    }

    public EmailVerificationToken getValidEmailVerificationToken(String rawToken) {
        String tokenHash = TokenUtils.hashToken(rawToken);
        return emailVerificationTokenRepository
                .findByTokenHashAndExpiresAtAfterAndUsedAtIsNull(tokenHash, Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired token"));
    }

    public User consumeEmailVerificationToken(String rawToken) {
        EmailVerificationToken token = this.getValidEmailVerificationToken(rawToken);
        boolean marked = this.emailVerificationTokenRepository.markUsed(token.getId());
        if(!marked)
            throw new AuthenticationException("Invalid or expired token");

        return token.getUser();
    }

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String rawToken) {
        String html = this.buildVerificationEmailHtml(firstName, toEmail, rawToken);
        this.emailService.sendHtmlEmail(toEmail, "Verify your email", html);
        log.info("Verification email sent to {}", toEmail);
    }


    private String buildVerificationEmailHtml(String firstName, String email, String rawToken) {
        String verificationLink = this.frontendUrl + this.verifyEmailPath + "?token=" + rawToken;

        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("email", email);
        context.setVariable("verificationLink", verificationLink);

        return emailService.loadTemplate("email/verify-email", context);
    }

}
