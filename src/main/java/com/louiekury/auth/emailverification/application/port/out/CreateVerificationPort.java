package com.louiekury.auth.emailverification.application.port.out;

import java.time.Instant;

public interface CreateVerificationPort {
    Instant createVerification(String email, String code, String id, String clientIp);
}
