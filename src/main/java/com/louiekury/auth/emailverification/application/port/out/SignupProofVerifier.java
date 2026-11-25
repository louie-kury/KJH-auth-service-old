package com.louiekury.auth.emailverification.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface SignupProofVerifier {
    Optional<VerifiedProof> verifySignupProof(String email, String token);

    record VerifiedProof(String proofDigest, Instant expiresAt) {}
}
