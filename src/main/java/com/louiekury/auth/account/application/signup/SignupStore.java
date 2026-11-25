package com.louiekury.auth.account.application.signup;

import java.time.Instant;
import java.util.Optional;

public interface SignupStore {
    Optional<CompletedSignup> findCompleted(byte[] keyDigest);
    boolean isProofUsed(byte[] proofDigest);
    boolean accountConflicts(SignupCommand command);
    String create(byte[] keyDigest, byte[] requestDigest, byte[] proofDigest, Instant proofExpiresAt,
                  SignupCommand command, String passwordHash, String accountStatus, String userType);

    record CompletedSignup(byte[] requestDigest, String memberId) { }
}
