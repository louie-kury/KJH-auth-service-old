package com.louiekury.auth.emailverification.application.port.out;

import java.time.Instant;

public interface VerificationStore {
    Instant reserve(String email, String code, String id, String clientIp);
}
