package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.createaccount.CreateAccountData;
import com.louiekury.auth.account.application.port.out.CreateAccountPort;
import com.louiekury.auth.account.application.port.out.GetRegisteredEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements CreateAccountPort, GetRegisteredEmailPort {

    private final AccountJpaRepository accountRepository;
    private final LoginCredentialJpaRepository credentialRepository;


    @Override
    public boolean getRegisteredEmailExists(String email) {
        return accountRepository.findByEmail(email).isPresent();
    }

    @Override
    public String createAccount(CreateAccountData account) {
        AccountJpaEntity savedAccount = accountRepository.save(new AccountJpaEntity(
                account.getUuid(), account.getStatus(), account.getName(), account.getPhoneNumber(),
                account.getGender(), account.getBirthDate(), account.getAddress(), account.getEmail(),
                account.isTermsAgreed()
        ));
        credentialRepository.save(new LoginCredentialJpaEntity(
                savedAccount.getId(), account.getLoginId(), account.getPasswordHash()
        ));
        return savedAccount.getUuid();
    }
}
