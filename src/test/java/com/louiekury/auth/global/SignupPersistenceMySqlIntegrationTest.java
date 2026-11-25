package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.out.persistence.SignupPersistenceAdapter;
import com.louiekury.auth.account.application.signup.SignupCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEST_SIGNUP_MYSQL_PORT", matches = "[0-9]+")
class SignupPersistenceMySqlIntegrationTest {
    private static JdbcTemplate jdbc;
    private static SignupPersistenceAdapter store;

    @BeforeAll
    static void createIsolatedSchema() {
        var ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:" + System.getenv("TEST_SIGNUP_MYSQL_PORT") + "/signup_test", "root", "signup-test-only");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE IF NOT EXISTS account (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL, name VARCHAR(100) NOT NULL, phone_number VARCHAR(30) NOT NULL, gender VARCHAR(30) NOT NULL, birth_date DATE NOT NULL, address VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE, terms_agreed BOOLEAN NOT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS login_credential (id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, login_id VARCHAR(100) NOT NULL UNIQUE, password_hash VARCHAR(255), created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_profile (id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, nickname VARCHAR(50) NOT NULL UNIQUE, user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('CAREGIVER', 'VETERINARIAN')), created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        store = new SignupPersistenceAdapter(jdbc, new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM login_credential");
        jdbc.update("DELETE FROM user_profile");
        jdbc.update("DELETE FROM account");
    }

    static SignupCommand command(String suffix) {
        return new SignupCommand("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul", "nick" + suffix,
            "login" + suffix, "long-password-12345", "test" + suffix + "@example.com", "A".repeat(43));
    }
    static int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test
    void successfulTransactionSavesAccountCredentialAndProfile() {
        store.create(command("1"), "hash", "ACTIVE", "CAREGIVER");
        for (String table : List.of("account", "login_credential", "user_profile")) assertThat(count(table)).isEqualTo(1);
    }

    @Test
    void uniqueNicknameConflictRollsBackAccountAndCredential() {
        store.create(command("1"), "hash", "ACTIVE", "CAREGIVER");
        var conflicting = new SignupCommand("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul", "nick1", "login2", "long-password-12345", "test2@example.com", "A".repeat(43));
        assertThatThrownBy(() -> store.create(conflicting, "hash", "ACTIVE", "CAREGIVER")).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        for (String table : List.of("account", "login_credential", "user_profile")) assertThat(count(table)).isEqualTo(1);
    }
}
