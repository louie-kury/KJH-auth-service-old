package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.availability.AvailabilityLookup;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityPersistenceAdapter implements AvailabilityLookup {
    private final JdbcTemplate jdbc;

    public AvailabilityPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean loginIdExists(String loginId) {
        return Objects.requireNonNull(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM login_credential WHERE login_id = ?)", Boolean.class, loginId));
    }

    @Override
    public boolean nicknameExists(String nickname) {
        return Objects.requireNonNull(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM user_profile WHERE nickname = ?)", Boolean.class, nickname));
    }
}
