package com.louiekury.auth.emailverification.application.confirm;

import java.time.Instant;

public interface ConfirmVerificationUseCase {
    Result confirm(String verificationId, String code);
    record Result(String emailVerificationToken, Instant expiresAt) {}
}
