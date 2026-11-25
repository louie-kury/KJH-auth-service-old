package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.signup.SignupCommand;
import com.louiekury.auth.account.application.signup.SignupStore;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class SignupPersistenceAdapter implements SignupStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public SignupPersistenceAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }

    @Override
    public Optional<AccountConflict> findAccountConflict(SignupCommand command) {
        Integer conflict = jdbc.queryForObject("SELECT CASE WHEN EXISTS(SELECT 1 FROM account WHERE email = ?) THEN 1 "
                        + "WHEN EXISTS(SELECT 1 FROM login_credential WHERE login_id = ?) THEN 2 "
                        + "WHEN EXISTS(SELECT 1 FROM user_profile WHERE nickname = ?) THEN 3 ELSE 0 END",
                Integer.class, command.email().trim(), command.loginId(), command.nickname());
        if (conflict == null || conflict == 0) return Optional.empty();
        return switch (conflict) {
            case 1 -> Optional.of(AccountConflict.EMAIL);
            case 2 -> Optional.of(AccountConflict.LOGIN_ID);
            case 3 -> Optional.of(AccountConflict.NICKNAME);
            default -> throw new IllegalStateException("Unexpected account conflict classification");
        };
    }
    @Override
    public String create(SignupCommand command, String passwordHash, String accountStatus, String userType) {
        return transaction.execute(status -> {
            String memberId = UUID.randomUUID().toString();
            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement = connection.prepareStatement("INSERT INTO account (uuid,status,name,phone_number,gender,birth_date,address,email,terms_agreed,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, memberId);
                statement.setString(2, accountStatus);
                statement.setString(3, command.name());
                statement.setString(4, command.phoneNumber());
                statement.setString(5, command.gender());
                statement.setObject(6, command.birthDate());
                statement.setString(7, command.address());
                statement.setString(8, command.email().trim());
                statement.setBoolean(9, true);
                return statement;
            }, keyHolder);
            Number accountId = keyHolder.getKey();
            if (accountId == null) throw new IllegalStateException("Account key missing");
            jdbc.update("INSERT INTO login_credential (account_id,login_id,password_hash,created_at,updated_at) VALUES (?,?,?,NOW(),NOW())",
                    accountId.longValue(), command.loginId(), passwordHash);
            jdbc.update("INSERT INTO user_profile (account_id,nickname,user_type,created_at,updated_at) VALUES (?,?,?,NOW(),NOW())",
                    accountId.longValue(), command.nickname(), userType);
            return memberId;
        });
    }
}
