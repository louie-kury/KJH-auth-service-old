package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.port.out.SaveAccountPort;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceAdapter implements SaveAccountPort {

    private final AccountJpaRepository accountRepository;
    private final LoginCredentialJpaRepository credentialRepository;

    public AccountPersistenceAdapter(
            AccountJpaRepository accountRepository,
            LoginCredentialJpaRepository credentialRepository
    ) {
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public String save(AccountData account) {
        AccountJpaEntity savedAccount = accountRepository.save(new AccountJpaEntity(
                account.uuid(), account.status(), account.name(), account.phoneNumber(),
                account.gender(), account.birthDate(), account.address(), account.email(), account.termsAgreed()
        ));
        credentialRepository.save(new LoginCredentialJpaEntity(
                savedAccount.getId(), account.loginId(), account.passwordHash()
        ));
        return savedAccount.getUuid();
    }
}
