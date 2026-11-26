package com.louiekury.auth.emailverification.application.port.out;
public interface UpdateSignupProofPort {
    boolean updateSignupProofConsumed(String email, String token);
}
