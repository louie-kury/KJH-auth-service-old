package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.createsignup.CreateSignupCommand;
import com.louiekury.auth.account.application.port.out.CreateSignupPort;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class SignupPersistenceAdapter implements CreateSignupPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public SignupPersistenceAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }

    @Override
    public Optional<AccountConflict> getAccountConflict(CreateSignupCommand command) {
        try {
            Integer conflict = jdbc.queryForObject(
                "SELECT CASE WHEN EXISTS(SELECT 1 FROM account WHERE email = ?) THEN 1 "
                    + "WHEN EXISTS(SELECT 1 FROM login_credential WHERE login_id = ?) THEN 2 "
                    + "WHEN EXISTS(SELECT 1 FROM user_profile WHERE nickname = ?) THEN 3 ELSE 0 END",
                Integer.class, command.getEmail().trim(), command.getLoginId(), command.getNickname());
            if (conflict == null || conflict == 0) {
                return Optional.empty();
            }
            return switch (conflict) {
                case 1 -> Optional.of(AccountConflict.EMAIL);
                case 2 -> Optional.of(AccountConflict.LOGIN_ID);
                case 3 -> Optional.of(AccountConflict.NICKNAME);
                default -> throw new IllegalStateException("Unexpected account conflict classification");
            };
        } catch (DataAccessException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public String createSignup(CreateSignupCommand command, String passwordHash, String accountStatus,
            String userType) {
        try {
            return transaction.execute(status -> {
                String memberId = UUID.randomUUID().toString();
                GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
                jdbc.update(connection -> {
                    var statement = connection.prepareStatement(
                        "INSERT INTO account (uuid,status,name,phone_number,gender,birth_date,address,email,"
                            + "terms_agreed,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                        Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, memberId);
                    statement.setString(2, accountStatus);
                    statement.setString(3, command.getName());
                    statement.setString(4, command.getPhoneNumber());
                    statement.setString(5, command.getGender());
                    statement.setObject(6, command.getBirthDate());
                    statement.setString(7, command.getAddress());
                    statement.setString(8, command.getEmail().trim());
                    statement.setBoolean(9, true);
                    return statement;
                }, keyHolder);
                Number accountId = keyHolder.getKey();
                if (accountId == null) {
                    throw new IllegalStateException("Account key missing");
                }
                jdbc.update("INSERT INTO login_credential (account_id,login_id,password_hash,created_at,updated_at) "
                        + "VALUES (?,?,?,NOW(),NOW())", accountId.longValue(), command.getLoginId(), passwordHash);
                jdbc.update("INSERT INTO user_profile (account_id,nickname,user_type,created_at,updated_at) "
                        + "VALUES (?,?,?,NOW(),NOW())", accountId.longValue(), command.getNickname(), userType);
                return memberId;
            });
        } catch (DuplicateKeyException e) {
            throw new DuplicateAccountException(e);
        } catch (DataAccessException e) {
            throw new StorageException(e);
        }
    }
}
