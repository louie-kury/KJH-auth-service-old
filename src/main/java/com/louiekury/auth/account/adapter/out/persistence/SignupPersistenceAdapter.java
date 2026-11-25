package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.application.signup.SignupCommand;
import com.louiekury.auth.account.application.signup.SignupStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    public Optional<CompletedSignup> findCompleted(byte[] keyDigest) {
        return jdbc.query("SELECT request_digest, member_uuid FROM signup_idempotency WHERE key_digest = ? AND expires_at > UTC_TIMESTAMP(6)",
                (rs, row) -> new CompletedSignup(rs.getBytes(1), rs.getString(2)), keyDigest).stream().findFirst();
    }

    @Override
    public boolean isProofUsed(byte[] proofDigest) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM signup_idempotency WHERE proof_digest = ?", Integer.class, proofDigest);
        return count != null && count > 0;
    }

    @Override
    public boolean accountConflicts(SignupCommand command) {
        Integer count = jdbc.queryForObject("SELECT (EXISTS(SELECT 1 FROM account WHERE email = ?) OR EXISTS(SELECT 1 FROM login_credential WHERE login_id = ?) OR EXISTS(SELECT 1 FROM user_profile WHERE nickname = ?))",
                Integer.class, command.email().trim(), command.loginId(), command.nickname());
        return count != null && count > 0;
    }
    @Override
    public String create(byte[] keyDigest, byte[] requestDigest, byte[] proofDigest, Instant proofExpiresAt,
                         SignupCommand command, String passwordHash, String accountStatus, String userType) {
        return transaction.execute(status -> {
            // Reclaim the key only after its replay window, while holding any affected row lock.
            jdbc.update("DELETE FROM signup_idempotency WHERE key_digest = ? AND expires_at <= UTC_TIMESTAMP(6)", keyDigest);
            String memberId = UUID.randomUUID().toString();
            int inserted = jdbc.update("INSERT INTO signup_idempotency (key_digest, proof_digest, request_digest, member_uuid, completed_at, expires_at) "
                            + "SELECT ?, ?, ?, ?, UTC_TIMESTAMP(6), DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 25 HOUR) WHERE ? > UTC_TIMESTAMP(6)",
                    keyDigest, proofDigest, requestDigest, memberId, LocalDateTime.ofInstant(proofExpiresAt, ZoneOffset.UTC));
            if (inserted != 1) throw new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION);
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
            jdbc.update("UPDATE signup_idempotency SET completed_at = UTC_TIMESTAMP(6), expires_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 25 HOUR) WHERE key_digest = ?", keyDigest);
            return memberId;
        });
    }
}
