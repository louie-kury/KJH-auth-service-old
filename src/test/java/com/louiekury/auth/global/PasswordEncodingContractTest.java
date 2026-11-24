package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.out.security.PasswordEncodingConfig;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

class PasswordEncodingContractTest {
    @Test
    void usesArgon2idWithIndependentSaltsAndVerifiedParameters() {
        PasswordEncoder encoder = new PasswordEncodingConfig().passwordEncoder();
        String password = "  A long password 😀  ";
        String first = encoder.encode(password);
        String second = encoder.encode(password);
        assertThat(first).startsWith("{argon2id}$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(second).isNotEqualTo(first);
        String[] parts = first.split("\\$");
        assertThat(Base64.getDecoder().decode(parts[4])).hasSize(16);
        assertThat(Base64.getDecoder().decode(parts[5])).hasSize(32);
        assertThat(encoder.matches(password, first)).isTrue();
        assertThat(encoder.matches(password, second)).isTrue();
        assertThat(encoder.matches(password.strip(), first)).isFalse();
        assertThat(encoder.matches("wrong password 123", first)).isFalse();
    }
}