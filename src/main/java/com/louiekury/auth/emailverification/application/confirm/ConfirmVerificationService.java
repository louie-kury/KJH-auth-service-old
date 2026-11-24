package com.louiekury.auth.emailverification.application.confirm;

import com.louiekury.auth.emailverification.application.port.out.VerificationConfirmationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class ConfirmVerificationService implements ConfirmVerificationUseCase {
    private final VerificationConfirmationStore store;
    private final SecureRandom random = new SecureRandom();

    public ConfirmVerificationService(VerificationConfirmationStore store) {
        this.store = store;
    }

    public Result confirm(String verificationId, String code) {
        if (verificationId == null || !verificationId.matches("verify_[A-Za-z0-9_-]{1,100}")
                || code == null || !code.matches("[0-9]{6}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Result(token, store.confirm(verificationId, code, token));
    }
}
