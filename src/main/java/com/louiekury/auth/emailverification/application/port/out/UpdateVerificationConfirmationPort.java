package com.louiekury.auth.emailverification.application.port.out;

import java.time.Instant;

public interface UpdateVerificationConfirmationPort {
    Instant updateVerificationConfirmed(String verificationId, String code, String token);
}
