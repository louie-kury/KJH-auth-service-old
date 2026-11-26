package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.port.out.GetAccountAvailabilityPort;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityPersistenceAdapter implements GetAccountAvailabilityPort {
    private final JdbcTemplate jdbc;


    @Override
    public boolean getLoginIdExists(String loginId) {
        return Objects.requireNonNull(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM login_credential WHERE login_id = ?)", Boolean.class, loginId));
    }

    @Override
    public boolean getNicknameExists(String nickname) {
        return Objects.requireNonNull(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM user_profile WHERE nickname = ?)", Boolean.class, nickname));
    }
}
