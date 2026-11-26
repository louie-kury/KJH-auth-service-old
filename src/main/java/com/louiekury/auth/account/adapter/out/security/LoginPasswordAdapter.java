package com.louiekury.auth.account.adapter.out.security;

import com.louiekury.auth.account.application.port.out.GetPasswordMatchPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LoginPasswordAdapter implements GetPasswordMatchPort {
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public LoginPasswordAdapter(PasswordEncoder passwords) {
        this.passwords = passwords;
        this.dummyHash = passwords.encode("dummy-password-not-a-user-secret");
    }

    @Override
    public boolean getPasswordMatch(String password, String hash) {
        try {
            return passwords.matches(password, hash == null ? dummyHash : hash) && hash != null;
        } catch (IllegalArgumentException ex) {
            passwords.matches(password, dummyHash);
            return false;
        }
    }
}
