package com.louiekury.auth.emailverification.application.updateemailverification;

public interface UpdateEmailVerificationUseCase {
    ConfirmVerificationResult updateEmailVerificationConfirmed(String verificationId, String code);
}
