package com.mateirobescu.thesis.auth.extensionauthcode;

public record ExtensionSignInCommand(
        String authCode,
        String codeVerifier
) {
    public static ExtensionSignInCommand fromRequest(ExtensionSignInRequest request) {
        return new ExtensionSignInCommand(request.authCode(), request.codeVerifier());
    }
}
