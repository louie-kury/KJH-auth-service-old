package com.louiekury.auth.account.adapter.out.security;

import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordHashAdapter implements CreatePasswordHashPort {

    private final PasswordEncoder passwordEncoder;


    @Override
    public String createPasswordHash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
