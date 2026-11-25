package com.louiekury.auth.emailverification.application.port.out;
public interface SignupProofVerifier {
    boolean consumeSignupProof(String email, String token);
}
