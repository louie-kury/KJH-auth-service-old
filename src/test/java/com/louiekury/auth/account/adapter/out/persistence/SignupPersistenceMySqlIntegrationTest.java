package com.louiekury.auth.account.adapter.out.persistence;

import com.louiekury.auth.account.adapter.out.persistence.SignupPersistenceAdapter;
import com.louiekury.auth.account.application.port.out.CreateSignupPort.DuplicateAccountException;
import com.louiekury.auth.account.application.createsignup.CreateSignupCommand;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "TEST_SIGNUP_MYSQL_PORT", matches = "[0-9]+")
class SignupPersistenceMySqlIntegrationTest {
    private static JdbcTemplate jdbc;
    private static SignupPersistenceAdapter store;

    @BeforeAll
    static void createIsolatedSchema() {
        var ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:" + System.getenv("TEST_SIGNUP_MYSQL_PORT") +
            "/signup_test", "root", "signup-test-only");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE IF NOT EXISTS account (id BIGINT AUTO_INCREMENT PRIMARY K" +
            "EY, uuid VARCHAR(36) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL, nam" +
            "e VARCHAR(100) NOT NULL, phone_number VARCHAR(30) NOT NULL, gender VAR" +
            "CHAR(30) NOT NULL, birth_date DATE NOT NULL, address VARCHAR(255) NOT " +
            "NULL, email VARCHAR(255) NOT NULL UNIQUE, terms_agreed BOOLEAN NOT NUL" +
            "L, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS login_credential (id BIGINT AUTO_INCREMENT " +
            "PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, login_id VARCHAR(100) " +
            "NOT NULL UNIQUE, password_hash VARCHAR(255), created_at DATETIME NOT N" +
            "ULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES " + "account(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_profile (id BIGINT AUTO_INCREMENT PRIM" +
            "ARY KEY, account_id BIGINT NOT NULL UNIQUE, nickname VARCHAR(50) NOT N" +
            "ULL UNIQUE, user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('CAREG" +
            "IVER', 'VETERINARIAN')), created_at DATETIME NOT NULL, updated_at DATE" +
            "TIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        store = new SignupPersistenceAdapter(jdbc, new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM login_credential");
        jdbc.update("DELETE FROM user_profile");
        jdbc.update("DELETE FROM account");
    }

    static CreateSignupCommand command(String suffix) {
        return CreateSignupCommand.of("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul", "nick" + suffix,
            "login" + suffix, "long-password-12345", "test" + suffix + "@example.com", "A".repeat(43));
    }
    static int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test
    @DisplayName("회원가입 성공 시 계정과 자격 정보와 프로필을 저장한다")
    void successfulTransactionSavesAccountCredentialAndProfile() {
        store.createSignup(command("1"), "hash", "ACTIVE", "CAREGIVER");
        for (String table : List.of("account", "login_credential", "user_profile"))
            assertThat(count(table)).isEqualTo(1);
    }

    @Test
    @DisplayName("닉네임 중복 시 계정과 자격 정보 저장을 롤백한다")
    void uniqueNicknameConflictRollsBackAccountAndCredential() {
        store.createSignup(command("1"), "hash", "ACTIVE", "CAREGIVER");
        var conflicting = CreateSignupCommand.of("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul",
            "nick1", "login2", "long-password-12345", "test2@example.com", "A".repeat(43));
        assertThatThrownBy(() -> store.createSignup(conflicting, "hash", "ACTIVE",
            "CAREGIVER"))
            .isInstanceOf(DuplicateAccountException.class)
            .hasCauseInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        for (String table : List.of("account", "login_credential", "user_profile"))
            assertThat(count(table)).isEqualTo(1);
    }
}
