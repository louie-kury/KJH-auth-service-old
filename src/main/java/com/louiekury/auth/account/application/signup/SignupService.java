package com.louiekury.auth.account.application.signup;

import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.emailverification.application.port.out.SignupProofVerifier;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SignupService implements SignupUseCase {
    private final SignupStore store;
    private final SignupProofVerifier proofVerifier;
    private final EncodePasswordPort passwords;

    public SignupService(SignupStore store, SignupProofVerifier proofVerifier, EncodePasswordPort passwords) {
        this.store = store;
        this.proofVerifier = proofVerifier;
        this.passwords = passwords;
    }

    @Override
    public String signup(SignupCommand command) {
        if (!proofVerifier.consumeSignupProof(command.email(), command.emailVerificationToken())) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }
        String passwordHash = passwords.encode(command.password());
        try {
            try {
                return store.create(command, passwordHash, "ACTIVE", "CAREGIVER");
            } catch (DuplicateKeyException e) {
                var conflict = store.findAccountConflict(command);
                if (conflict.isPresent()) {
                    ErrorCode code = switch (conflict.get()) {
                        case EMAIL -> ErrorCode.EMAIL_ALREADY_REGISTERED;
                        case LOGIN_ID -> ErrorCode.LOGIN_ID_ALREADY_EXISTS;
                        case NICKNAME -> ErrorCode.NICKNAME_ALREADY_EXISTS;
                    };
                    throw new BusinessException(code);
                }
                throw new BusinessException(ErrorCode.SIGNUP_PENDING);
            }
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SIGNUP_PENDING);
        }
    }
}
