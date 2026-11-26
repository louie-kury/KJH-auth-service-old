package com.louiekury.auth.emailverification.application.updatesignupproof;

import com.louiekury.auth.emailverification.application.port.out.UpdateSignupProofPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateSignupProofService implements UpdateSignupProofUseCase {
    private final UpdateSignupProofPort verifier;

    @Override
    public boolean updateSignupProofConsumed(String email, String token) {
        return verifier.updateSignupProofConsumed(email, token);
    }
}
