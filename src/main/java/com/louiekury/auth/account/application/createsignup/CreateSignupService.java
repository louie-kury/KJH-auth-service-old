package com.louiekury.auth.account.application.createsignup;

import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import com.louiekury.auth.account.application.port.out.CreateSignupPort;
import com.louiekury.auth.account.application.port.out.UpdateSignupProofStatusPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateSignupService implements CreateSignupUseCase {
    private final CreateSignupPort store;
    private final UpdateSignupProofStatusPort proofVerifier;
    private final CreatePasswordHashPort passwords;


    @Override
    public CreateSignupResult createSignup(CreateSignupCommand command) {
        if (!proofVerifier.updateSignupProofAsConsumed(command.getEmail(), command.getEmailVerificationToken())) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION);
        }
        String passwordHash = passwords.createPasswordHash(command.getPassword());
        try {
            try {
                return CreateSignupResult.of(store.createSignup(command, passwordHash, "ACTIVE", "CAREGIVER"));
            } catch (CreateSignupPort.DuplicateAccountException e) {
                var conflict = store.getAccountConflict(command);
                if (conflict.isPresent()) {
                    ErrorCode code = switch (conflict.get()) {
                        case EMAIL -> ErrorCode.EMAIL_ALREADY_EXISTS;
                        case LOGIN_ID -> ErrorCode.LOGIN_ID_ALREADY_EXISTS;
                        case NICKNAME -> ErrorCode.NICKNAME_ALREADY_EXISTS;
                    };
                    throw new BusinessException(code);
                }
                throw new BusinessException(ErrorCode.SIGNUP_PENDING);
            }
        } catch (CreateSignupPort.StorageException e) {
            throw new BusinessException(ErrorCode.SIGNUP_PENDING);
        }
    }
}
