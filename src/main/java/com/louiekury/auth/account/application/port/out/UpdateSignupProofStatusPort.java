package com.louiekury.auth.account.application.port.out;

public interface UpdateSignupProofStatusPort {
    boolean updateSignupProofAsConsumed(String email, String token);
}
