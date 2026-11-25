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
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_profile (id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, nickname VARCHAR(50) NOT NULL UNIQUE, user_type VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS signup_idempotency (id BIGINT AUTO_INCREMENT PRIMARY KEY, key_digest BINARY(32) NOT NULL UNIQUE, proof_digest BINARY(32) NOT NULL UNIQUE, request_digest BINARY(32) NOT NULL, member_uuid CHAR(36) NOT NULL, completed_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NOT NULL, INDEX(expires_at))");
        store = new SignupPersistenceAdapter(jdbc, new DataSourceTransactionManager(ds));
    }

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM login_credential");
        jdbc.update("DELETE FROM user_profile");
        jdbc.update("DELETE FROM account");
        jdbc.update("DELETE FROM signup_idempotency");
    }

    static byte[] digest(int n) { byte[] b = new byte[32]; b[0] = (byte)n; return b; }
    static SignupCommand command(String suffix) {
        return new SignupCommand("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul", "nick" + suffix,
            "login" + suffix, "long-password-12345", "test" + suffix + "@example.com", "A".repeat(43));
    }
    static int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test
    void successfulTransactionSavesAccountProfileAndRetryRecord() {
        String id = store.create(digest(1), digest(2), digest(3), Instant.now().plusSeconds(300), command("1"), "hash", "ACTIVE", "USER");
        assertThat(store.findCompleted(digest(1)).orElseThrow().memberId()).isEqualTo(id);
        assertThat(store.findCompleted(digest(1)).orElseThrow().requestDigest()).containsExactly(digest(2));
        assertThat(store.isProofUsed(digest(3))).isTrue();
        for (String table : List.of("account", "login_credential", "user_profile", "signup_idempotency")) assertThat(count(table)).isEqualTo(1);
    }

    @Test
    void profileConflictRollsBackProofAndAccountAndAllowsRetry() {
        store.create(digest(1), digest(2), digest(3), Instant.now().plusSeconds(300), command("1"), "hash", "ACTIVE", "USER");
        var conflicting = new SignupCommand("Tester", "01012345678", "MALE", LocalDate.of(1995,1,1), "Seoul", "nick1", "login2", "long-password-12345", "test2@example.com", "A".repeat(43));
        assertThatThrownBy(() -> store.create(digest(4), digest(5), digest(6), Instant.now().plusSeconds(300), conflicting, "hash", "ACTIVE", "USER")).isInstanceOf(RuntimeException.class);
        assertThat(store.isProofUsed(digest(6))).isFalse();
        assertThat(store.findCompleted(digest(4))).isEmpty();
        for (String table : List.of("account", "login_credential", "user_profile", "signup_idempotency")) assertThat(count(table)).isEqualTo(1);
        store.create(digest(4), digest(5), digest(6), Instant.now().plusSeconds(300), command("2"), "hash", "ACTIVE", "USER");
        assertThat(count("account")).isEqualTo(2);
    }

    @Test
    void concurrentUseOfSameProofCreatesOnlyOneAccount() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var tasks = List.<java.util.concurrent.Callable<Boolean>>of(
                () -> tryCreate(10, command("1")), () -> tryCreate(11, command("2")));
            var outcomes = pool.invokeAll(tasks);
            assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get())).containsExactlyInAnyOrder(true, false);
            assertThat(count("account")).isEqualTo(1);
            assertThat(count("signup_idempotency")).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    private static boolean tryCreate(int key, SignupCommand command) {
        try { store.create(digest(key), digest(key + 10), digest(99), Instant.now().plusSeconds(300), command, "hash", "ACTIVE", "USER"); return true; }
        catch (RuntimeException e) { return false; }
    }

    private static String cleanupSql() throws Exception {
        try (var in = SignupPersistenceMySqlIntegrationTest.class.getResourceAsStream("/db/manual/002-cleanup-expired-signup-idempotency.sql")) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @Test
    void cleanupDeletesExpiredRecordButKeepsUnexpiredRecord() throws Exception {
        store.create(digest(1), digest(2), digest(3), Instant.now().plusSeconds(300), command("1"), "hash", "ACTIVE", "USER");
        store.create(digest(4), digest(5), digest(6), Instant.now().plusSeconds(300), command("2"), "hash", "ACTIVE", "USER");
        jdbc.update("UPDATE signup_idempotency SET expires_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND) WHERE key_digest = ?", digest(1));
        assertThat(jdbc.update(cleanupSql())).isEqualTo(1);
        assertThat(store.findCompleted(digest(1))).isEmpty();
        assertThat(store.findCompleted(digest(4))).isPresent();
        assertThat(store.isProofUsed(digest(6))).isTrue();
    }

    @Test
    void cleanupWaitsForInFlightExtensionAndKeepsTheExtendedRecord() throws Exception {
        store.create(digest(1), digest(2), digest(3), Instant.now().plusSeconds(300), command("1"), "hash", "ACTIVE", "USER");
        jdbc.update("UPDATE signup_idempotency SET expires_at = DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 1 SECOND) WHERE key_digest = ?", digest(1));
        var pool = Executors.newSingleThreadExecutor();
        try (var connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (var update = connection.prepareStatement("UPDATE signup_idempotency SET expires_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 1 HOUR) WHERE key_digest = ?")) {
                update.setBytes(1, digest(1));
                update.executeUpdate();
            }
            var started = new java.util.concurrent.CountDownLatch(1);
            var delete = pool.submit(() -> { started.countDown(); return jdbc.update(cleanupSql()); });
            assertThat(started.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> delete.get(300, java.util.concurrent.TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);
            connection.commit();
            assertThat(delete.get(5, java.util.concurrent.TimeUnit.SECONDS)).isZero();
            assertThat(store.findCompleted(digest(1))).isPresent();
        } finally { pool.shutdownNow(); }
    }
    @Test
    void schedulerDeletesAtMostFiveHundredExpiredRecordsAndPreservesUnexpired() {
        var args = new java.util.ArrayList<Object[]>();
        for (int i = 0; i < 501; i++) {
            byte[] key = java.nio.ByteBuffer.allocate(32).putInt(i).array();
            byte[] proof = java.nio.ByteBuffer.allocate(32).putInt(i + 1000).array();
            args.add(new Object[]{key, proof, digest(88), "00000000-0000-0000-0000-000000000000"});
        }
        jdbc.batchUpdate("INSERT INTO signup_idempotency (key_digest, proof_digest, request_digest, member_uuid, completed_at, expires_at) VALUES (?,?,?,?,UTC_TIMESTAMP(6),DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND))", args);
        store.create(digest(90), digest(91), digest(92), Instant.now().plusSeconds(300), command("live"), "hash", "ACTIVE", "USER");
        new com.louiekury.auth.account.adapter.out.persistence.SignupCleanupScheduler(jdbc).cleanupExpired();
        assertThat(count("signup_idempotency")).isEqualTo(2);
        assertThat(store.findCompleted(digest(90))).isPresent();
        new com.louiekury.auth.account.adapter.out.persistence.SignupCleanupScheduler(jdbc).cleanupExpired();
        assertThat(count("signup_idempotency")).isEqualTo(1);
        assertThat(store.findCompleted(digest(90))).isPresent();
    }
    @Test
    void expiredProofCannotBeCommitted() {
        assertThatThrownBy(() -> store.create(digest(1), digest(2), digest(3), Instant.now().minusSeconds(1), command("1"), "hash", "ACTIVE", "USER")).isInstanceOf(RuntimeException.class);
        assertThat(count("account")).isZero();
        assertThat(count("signup_idempotency")).isZero();
    }
}
