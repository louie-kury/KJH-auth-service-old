package com.louiekury.auth.account.application.signup;

import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.emailverification.application.port.out.SignupProofVerifier;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.util.HexFormat;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SignupService implements SignupUseCase {
    private final SignupStore store;
    private final SignupProofVerifier proofVerifier;
    private final SignupDigests digests;
    private final EncodePasswordPort passwords;

    public SignupService(SignupStore store, SignupProofVerifier proofVerifier, SignupDigests digests,
                         EncodePasswordPort passwords) {
        this.store = store;
        this.proofVerifier = proofVerifier;
        this.digests = digests;
        this.passwords = passwords;
    }

    @Override
    public String signup(String key, SignupCommand command) {
        byte[] keyDigest = digests.keyDigest(key);
        byte[] requestDigest = digests.requestDigest(command);
        try {
            var completed = store.findCompleted(keyDigest);
            if (completed.isPresent()) return replay(completed.get(), requestDigest);
            var proof = proofVerifier.verifySignupProof(command.email(), command.emailVerificationToken())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION));
            byte[] proofDigest = HexFormat.of().parseHex(proof.proofDigest());
            String passwordHash = passwords.encode(command.password());
            try {
                return store.create(keyDigest, requestDigest, proofDigest, proof.expiresAt(), command,
                        passwordHash, "ACTIVE", "USER");
            } catch (DuplicateKeyException e) {
                // Unique conflicts abort and roll back the first transaction before this lookup.
                completed = store.findCompleted(keyDigest);
                if (completed.isPresent()) return replay(completed.get(), requestDigest);
                if (store.isProofUsed(proofDigest)) throw new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION);
                if (store.accountConflicts(command)) throw new BusinessException(ErrorCode.ACCOUNT_CONFLICT);
                throw new BusinessException(ErrorCode.SIGNUP_PENDING);
            }
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SIGNUP_PENDING);
        }
    }

    private String replay(SignupStore.CompletedSignup saved, byte[] requestDigest) {
        if (!digests.matches(saved.requestDigest(), requestDigest)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return saved.memberId();
    }
}
