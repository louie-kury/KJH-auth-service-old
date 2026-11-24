package com.louiekury.auth.emailverification.application.port.out;

import java.time.Instant;

public interface VerificationConfirmationStore {
    Instant confirm(String verificationId, String code, String token);
}
