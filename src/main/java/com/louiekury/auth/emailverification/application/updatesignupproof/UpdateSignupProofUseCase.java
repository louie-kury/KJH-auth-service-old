package com.louiekury.auth.emailverification.application.updatesignupproof;

public interface UpdateSignupProofUseCase {
    boolean updateSignupProofConsumed(String email, String token);
}
