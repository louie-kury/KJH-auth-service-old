package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.adapter.out.persistence.SignupPersistenceAdapter;
import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.account.application.signup.SignupDigests;
import com.louiekury.auth.account.application.signup.SignupService;
import com.louiekury.auth.emailverification.application.port.out.SignupProofVerifier;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "TEST_SIGNUP_MYSQL_PORT", matches = "[0-9]+")
class SignupDuplicateMySqlWebTest {
    private static JdbcTemplate db;
    private MockMvc mvc;

    @BeforeAll
    static void schema() {
        var ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:" + System.getenv("TEST_SIGNUP_MYSQL_PORT") + "/signup_test", "root", "signup-test-only");
        db = new JdbcTemplate(ds);
        db.execute("CREATE TABLE IF NOT EXISTS account (id BIGINT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL, name VARCHAR(100) NOT NULL, phone_number VARCHAR(30) NOT NULL, gender VARCHAR(30) NOT NULL, birth_date DATE NOT NULL, address VARCHAR(255) NOT NULL, email VARCHAR(255) COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE, terms_agreed BOOLEAN NOT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL)");
        db.execute("CREATE TABLE IF NOT EXISTS login_credential (id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, login_id VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE, password_hash VARCHAR(255), created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        db.execute("CREATE TABLE IF NOT EXISTS user_profile (id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, nickname VARCHAR(50) COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE, user_type VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFERENCES account(id))");
        db.execute("CREATE TABLE IF NOT EXISTS signup_idempotency (id BIGINT AUTO_INCREMENT PRIMARY KEY, key_digest BINARY(32) NOT NULL UNIQUE, proof_digest BINARY(32) NOT NULL UNIQUE, request_digest BINARY(32) NOT NULL, member_uuid CHAR(36) NOT NULL, completed_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NOT NULL, INDEX(expires_at))");
    }
    @BeforeEach
    void setUp() {
        db.update("DELETE FROM login_credential"); db.update("DELETE FROM user_profile"); db.update("DELETE FROM account"); db.update("DELETE FROM signup_idempotency");
        var store = new SignupPersistenceAdapter(db, new DataSourceTransactionManager(db.getDataSource()));
        SignupProofVerifier proofs = (email, token) -> token.equals("invalid") ? Optional.empty() : Optional.of(new SignupProofVerifier.VerifiedProof(token.substring(0, 1).repeat(64), Instant.now().plusSeconds(600)));
        var passwords = mock(EncodePasswordPort.class); when(passwords.encode(anyString())).thenReturn("test-hash");
        var digests = new SignupDigests("idempotency-secret-01234567890123456789", "email-key", "code-key");
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupService(store, proofs, digests, passwords))).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private String request(String email, String login, String nick, String token) {
        return "{\"name\":\"Tester\",\"phoneNumber\":\"01012345678\",\"gender\":\"MALE\",\"birthDate\":\"1995-01-01\",\"address\":\"Seoul\",\"nickname\":\"" + nick + "\",\"loginId\":\"" + login + "\",\"password\":\"long-password-12345\",\"email\":\"" + email + "\",\"emailVerificationToken\":\"" + token + "\"}";
    }
    private org.springframework.test.web.servlet.ResultActions submit(String key, String email, String login, String nick, String token) throws Exception {
        return mvc.perform(post("/api/auth/signup").header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(request(email, login, nick, token)));
    }
    private void seed() throws Exception { submit("seed", "first@example.com", "Resume", "Cafe", "a").andExpect(status().isCreated()); }
    @Test void emailConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other", "FIRST@example.com", "other-id", "other-nick", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED")); }
    @Test void loginConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other", "other@example.com", "résumé", "other-nick", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS")); }
    @Test void nicknameConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other", "other@example.com", "other-id", "café", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS")); }
    @Test void emailTakesPriorityOverBothOtherConflicts() throws Exception { seed(); submit("other", "FIRST@example.com", "résumé", "café", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED")); }
    @Test void loginTakesPriorityOverNicknameConflict() throws Exception { seed(); submit("other", "other@example.com", "résumé", "café", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS")); }
    @Test void invalidProofTakesPriorityOverDuplicateFields() throws Exception { seed(); submit("other", "first@example.com", "Resume", "Cafe", "invalid").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION")); }
    @Test void usedProofTakesPriorityOverDuplicateFields() throws Exception { seed(); submit("other", "first@example.com", "Resume", "Cafe", "a").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION")); }
    @Test void sameKeyDifferentInputTakesPriorityOverDuplicateFields() throws Exception { seed(); submit("seed", "first@example.com", "Resume", "Cafe", "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT")); }
    @Test void concurrentSameNicknameReturnsOneSuccessAndSpecificConflict() throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var a=pool.submit(() -> { start.await(); return submit("key-a", "a@example.com", "id-a", "same", "a").andReturn().getResponse(); });
            var b=pool.submit(() -> { start.await(); return submit("key-b", "b@example.com", "id-b", "same", "b").andReturn().getResponse(); });
            start.countDown(); var x=a.get(); var y=b.get();
            assertThat(java.util.List.of(x.getStatus(),y.getStatus())).containsExactlyInAnyOrder(201,409);
            var conflict=x.getStatus()==409?x:y; assertThat(conflict.getContentAsString()).contains("NICKNAME_ALREADY_EXISTS");
            assertThat(db.queryForObject("SELECT COUNT(*) FROM account", Integer.class)).isEqualTo(1);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM signup_idempotency", Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
