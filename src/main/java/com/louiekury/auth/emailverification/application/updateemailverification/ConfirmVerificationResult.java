package com.louiekury.auth.emailverification.application.updateemailverification;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ConfirmVerificationResult {
    private final String emailVerificationToken;
    private final Instant expiresAt;

    public static ConfirmVerificationResult of(String emailVerificationToken, Instant expiresAt) {
        return ConfirmVerificationResult.builder()
            .emailVerificationToken(emailVerificationToken)
            .expiresAt(expiresAt)
            .build();
    }
}
