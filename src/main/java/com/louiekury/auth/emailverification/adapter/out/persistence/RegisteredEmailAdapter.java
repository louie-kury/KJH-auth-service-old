package com.louiekury.auth.emailverification.adapter.out.persistence;

import com.louiekury.auth.account.adapter.out.persistence.AccountJpaRepository;
import com.louiekury.auth.emailverification.application.port.out.RegisteredEmailLookup;
import org.springframework.stereotype.Component;

@Component
public class RegisteredEmailAdapter implements RegisteredEmailLookup {
    private final AccountJpaRepository repository;
    public RegisteredEmailAdapter(AccountJpaRepository repository) { this.repository = repository; }
    public boolean exists(String email) { return repository.findByEmail(email).isPresent(); }
}
