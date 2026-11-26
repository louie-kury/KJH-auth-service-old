package com.louiekury.auth.emailverification.application.updateemailverification;

import com.louiekury.auth.emailverification.application.port.out.UpdateVerificationConfirmationPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UpdateEmailVerificationService implements UpdateEmailVerificationUseCase {
    private final UpdateVerificationConfirmationPort store;
    private final SecureRandom random = new SecureRandom();

    public ConfirmVerificationResult updateEmailVerificationConfirmed(String verificationId, String code) {
        if (verificationId == null || !verificationId.matches("verify_[A-Za-z0-9_-]{1,100}")
                || code == null || !code.matches("[0-9]{6}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return ConfirmVerificationResult.of(token, store.updateVerificationConfirmed(verificationId, code, token));
    }
}
