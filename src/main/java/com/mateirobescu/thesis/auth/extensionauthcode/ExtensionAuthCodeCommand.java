package com.mateirobescu.thesis.auth.extensionauthcode;

import com.mateirobescu.thesis.auth.AuthUtils;

import java.util.UUID;

public record ExtensionAuthCodeCommand(
        String codeChallenge,
        String codeChallengeMethod,
        UUID userId
) {
    public static ExtensionAuthCodeCommand fromRequest(ExtensionAuthCodeRequest request) {
        return new ExtensionAuthCodeCommand(
                request.codeChallenge(),
                request.codeChallengeMethod(),
                AuthUtils.getCurrentUserId()
        );
    }
}
