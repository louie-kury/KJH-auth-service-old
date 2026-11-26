package com.louiekury.auth.account.application.getregisteredemail;

import com.louiekury.auth.account.application.port.out.GetRegisteredEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRegisteredEmailService implements GetRegisteredEmailUseCase {
    private final GetRegisteredEmailPort port;

    @Override
    public boolean getRegisteredEmailExists(String email) {
        return port.getRegisteredEmailExists(email);
    }
}
