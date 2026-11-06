package com.mateirobescu.thesis.auth.extensionauthcode;

public record ExtensionAuthCodeResponse(
        String authCode
) {
    public static ExtensionAuthCodeResponse fromAuthCode(String authCode) {
        return new ExtensionAuthCodeResponse(authCode);
    }
}
