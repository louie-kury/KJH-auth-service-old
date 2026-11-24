package com.louiekury.auth.account.adapter.out.security;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordEncodingConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder argon2id = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
        return new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon2id));
    }
}
