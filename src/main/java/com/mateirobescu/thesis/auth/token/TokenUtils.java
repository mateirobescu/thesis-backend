package com.mateirobescu.thesis.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {

    public static String generateSecureRandomToken() {
        return generateSecureRandomToken(32);
    }

    public static String generateSecureRandomToken(int byteLength) {
        if (byteLength < 16) {
            throw new IllegalArgumentException("Token must be at least 16 bytes (128 bits) for adequate security");
        }

        byte[] randomBytes = new byte[byteLength];
        new SecureRandom().nextBytes(randomBytes);
        //TODO maybe do separate for url encoder?
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            //TODO maybe do separate for url encoder?
            //TDOO maybe different withotut padding?
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
