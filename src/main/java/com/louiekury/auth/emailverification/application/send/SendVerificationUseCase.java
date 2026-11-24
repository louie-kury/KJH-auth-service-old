package com.louiekury.auth.emailverification.application.send;

import java.time.Instant;

public interface SendVerificationUseCase {
    Result send(String email, String purpose, String clientIp);
    record Result(String verificationId, Instant expiresAt) {}
}
