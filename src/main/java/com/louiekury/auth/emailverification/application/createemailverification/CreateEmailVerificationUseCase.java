package com.louiekury.auth.emailverification.application.createemailverification;

public interface CreateEmailVerificationUseCase {
    SendVerificationResult createEmailVerification(String email, String purpose, String clientIp);
}
