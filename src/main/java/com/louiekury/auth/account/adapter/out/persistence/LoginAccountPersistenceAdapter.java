package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.login.GetLoginAccountResult;
import com.louiekury.auth.account.application.port.out.GetLoginAccountPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginAccountPersistenceAdapter implements GetLoginAccountPort {
    private final LoginCredentialJpaRepository credentials;
    private final AccountJpaRepository accounts;

    @Override
    public Optional<GetLoginAccountResult> getLoginAccount(String loginId) {
        return credentials.findByLoginId(loginId).flatMap(c -> accounts.findById(c.getAccountId())
            .map(a -> GetLoginAccountResult.of(a.getUuid(), c.getPasswordHash(),
                "ACTIVE".equals(a.getStatus()) && a.getDeletedAt() == null)));
    }
}
