package com.louiekury.auth.account.adapter.out.security;

import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashAdapter implements EncodePasswordPort {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
