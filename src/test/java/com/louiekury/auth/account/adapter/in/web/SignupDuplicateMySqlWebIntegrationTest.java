package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.adapter.in.web.SignupWebAdapter;
import com.louiekury.auth.account.adapter.out.persistence.SignupPersistenceAdapter;
import com.louiekury.auth.account.application.createsignup.CreateSignupService;
import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import com.louiekury.auth.account.application.port.out.UpdateSignupProofStatusPort;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "TEST_SIGNUP_MYSQL_PORT", matches = "[0-9]+")
class SignupDuplicateMySqlWebIntegrationTest {
    private static JdbcTemplate db;
    private MockMvc mvc;

    @BeforeAll
    static void schema() {
        var ds = new DriverManagerDataSource("jdbc:mysql://127.0.0.1:" + System.getenv("TEST_SIGNUP_MYSQL_PORT") +
            "/signup_test", "root", "signup-test-only");
        db = new JdbcTemplate(ds);
        db.execute("CREATE TABLE IF NOT EXISTS account (id BIGINT AUTO_INCREMENT PRIMARY K" +
            "EY, uuid VARCHAR(36) NOT NULL UNIQUE, status VARCHAR(30) NOT NULL, nam" +
            "e VARCHAR(100) NOT NULL, phone_number VARCHAR(30) NOT NULL, gender VAR" +
            "CHAR(30) NOT NULL, birth_date DATE NOT NULL, address VARCHAR(255) NOT " +
            "NULL, email VARCHAR(255) COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE, t" +
            "erms_agreed BOOLEAN NOT NULL, created_at DATETIME NOT NULL, updated_at" + " DATETIME NOT NULL)");
        db.execute("CREATE TABLE IF NOT EXISTS login_credential (id BIGINT AUTO_INCREMENT " +
            "PRIMARY KEY, account_id BIGINT NOT NULL UNIQUE, login_id VARCHAR(100) " +
            "COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE, password_hash VARCHAR(255)" +
            ", created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, FOREIGN " +
            "KEY(account_id) REFERENCES account(id))");
        db.execute("CREATE TABLE IF NOT EXISTS user_profile (id BIGINT AUTO_INCREMENT PRIM" +
            "ARY KEY, account_id BIGINT NOT NULL UNIQUE, nickname VARCHAR(50) COLLA" +
            "TE utf8mb4_0900_ai_ci NOT NULL UNIQUE, user_type VARCHAR(20) NOT NULL " +
            "CHECK (user_type IN ('CAREGIVER', 'VETERINARIAN')), created_at DATETIM" +
            "E NOT NULL, updated_at DATETIME NOT NULL, FOREIGN KEY(account_id) REFE" + "RENCES account(id))");
    }
    @BeforeEach
    void setUp() {
        db.update("DELETE FROM login_credential"); db.update("DELETE FROM user_profile");
            db.update("DELETE FROM account");
        var store = new SignupPersistenceAdapter(db, new DataSourceTransactionManager(db.getDataSource()));
        var used = java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
        UpdateSignupProofStatusPort proofs = (email, token) -> !token.equals("invalid") && used.add(token);
        var passwords = mock(CreatePasswordHashPort.class);
            when(passwords.createPasswordHash(anyString())).thenReturn("test-hash");
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupWebAdapter(new CreateSignupService(store,
            proofs, passwords)))).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private String request(String email, String login, String nick, String token) {
        return "{\"name\":\"Tester\",\"phoneNumber\":\"01012345678\",\"gender\":\"MALE" +
            "\",\"birthDate\":\"1995-01-01\",\"address\":\"Seoul\",\"nickname\":\"" + nick + "\",\"loginId\":\"" +
            login + "\",\"password\":\"long-password-12345\",\"email\":\"" + email + "\",\"emailVerificationToken\":\""
            + token + "\"}";
    }
    private org.springframework.test.web.servlet.ResultActions submit(String key, String email, String login, String
        nick, String token) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(request(email,
            login, nick, token)));
    }
    private void seed() throws Exception { submit("seed", "first@example.com", "Resume", "Cafe",
        "a").andExpect(status().isCreated()); }
    @Test
    @DisplayName("DB 정렬 규칙상 이메일이 중복되면 이메일 중복 오류를 반환한다")
    void emailConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other",
        "FIRST@example.com", "other-id", "other-nick",
        "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS")); }
    @Test
    @DisplayName("DB 정렬 규칙상 아이디가 중복되면 아이디 중복 오류를 반환한다")
    void loginConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other",
        "other@example.com", "résumé", "other-nick",
        "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS")); }
    @Test
    @DisplayName("DB 정렬 규칙상 닉네임이 중복되면 닉네임 중복 오류를 반환한다")
    void nicknameConflictReturnsSpecificErrorUsingDatabaseCollation() throws Exception { seed(); submit("other",
        "other@example.com", "other-id", "café",
        "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_EXISTS")); }
    @Test
    @DisplayName("이메일과 다른 필드가 중복되면 이메일 오류를 우선한다")
    void emailTakesPriorityOverBothOtherConflicts() throws Exception { seed(); submit("other", "FIRST@example.com",
        "résumé", "café",
        "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS")); }
    @Test
    @DisplayName("아이디와 닉네임이 중복되면 아이디 오류를 우선한다")
    void loginTakesPriorityOverNicknameConflict() throws Exception { seed(); submit("other", "other@example.com",
        "résumé", "café",
        "b").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS")); }
    @Test
    @DisplayName("증명이 유효하지 않으면 중복 오류보다 증명 오류를 우선한다")
    void invalidProofTakesPriorityOverDuplicateFields() throws Exception { seed(); submit("other", "first@example.com",
        "Resume", "Cafe",
        "invalid").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION"));
         }
    @Test
    @DisplayName("이미 사용한 증명은 중복 오류보다 증명 오류를 우선한다")
    void usedProofTakesPriorityOverDuplicateFields() throws Exception { seed(); submit("other", "first@example.com",
        "Resume", "Cafe",
        "a").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION")); }
    @Test
    @DisplayName("같은 닉네임의 동시 가입은 한 번 성공하고 한 번 충돌한다")
    void concurrentSameNicknameReturnsOneSuccessAndSpecificConflict() throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var a=pool.submit(() -> { start.await(); return submit("key-a", "a@example.com", "id-a", "same",
                "a").andReturn().getResponse(); });
            var b=pool.submit(() -> { start.await(); return submit("key-b", "b@example.com", "id-b", "same",
                "b").andReturn().getResponse(); });
            start.countDown(); var x=a.get(); var y=b.get();
            assertThat(java.util.List.of(x.getStatus(),y.getStatus())).containsExactlyInAnyOrder(201,409);
            var conflict=x.getStatus()==409?x:y;
                assertThat(conflict.getContentAsString()).contains("NICKNAME_ALREADY_EXISTS");
            assertThat(db.queryForObject("SELECT COUNT(*) FROM account", Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
