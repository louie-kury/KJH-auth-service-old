package com.louiekury.auth.global.adapter;

import com.louiekury.auth.account.application.port.out.UpdateSignupProofStatusPort;
import com.louiekury.auth.emailverification.application.updatesignupproof.UpdateSignupProofUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupProofBridgeAdapter implements UpdateSignupProofStatusPort {

    private final UpdateSignupProofUseCase updateSignupProofUseCase;

    @Override
    public boolean updateSignupProofAsConsumed(String email, String token) {
        return updateSignupProofUseCase.updateSignupProofConsumed(email, token);
    }
}
