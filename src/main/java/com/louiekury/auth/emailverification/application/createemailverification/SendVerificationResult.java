package com.louiekury.auth.emailverification.application.createemailverification;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class SendVerificationResult {
    private final String verificationId;
    private final Instant expiresAt;

    public static SendVerificationResult of(String verificationId, Instant expiresAt) {
        return SendVerificationResult.builder()
            .verificationId(verificationId)
            .expiresAt(expiresAt)
            .build();
    }
}
