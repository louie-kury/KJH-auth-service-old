package com.louiekury.auth.global.adapter;

import com.louiekury.auth.account.application.getregisteredemail.GetRegisteredEmailUseCase;
import com.louiekury.auth.emailverification.application.port.out.GetRegisteredEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisteredEmailBridgeAdapter implements GetRegisteredEmailPort {

    private final GetRegisteredEmailUseCase getRegisteredEmailUseCase;

    @Override
    public boolean getRegisteredEmailExists(String email) {
        return getRegisteredEmailUseCase.getRegisteredEmailExists(email);
    }
}
