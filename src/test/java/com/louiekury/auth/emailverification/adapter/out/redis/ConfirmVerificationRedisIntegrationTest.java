package com.louiekury.auth.emailverification.adapter.out.redis;

import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationAdapter;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HexFormat;
import java.util.stream.IntStream;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_PORT", matches = "[0-9]+")
class ConfirmVerificationRedisIntegrationTest {
    private static final String EMAIL_KEY = "email-key-012345678901234567890123456789";
    private static final String CODE_KEY = "code-key-0123456789012345678901234567890";
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static RedisVerificationAdapter store;

    @BeforeAll
    static void setUp() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("TEST_REDIS_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        store = new RedisVerificationAdapter(redis, EMAIL_KEY, CODE_KEY, 100, 3600);
    }

    @AfterAll
    static void tearDown() { if (factory != null) factory.destroy(); }

    private static String id() { return "verify_" + UUID.randomUUID(); }
    private static String email() { return UUID.randomUUID() + "@example.com"; }
    private static String hash(String key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static void expect(ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("인증 성공 시 동일 이메일에 10분 증명을 발급하고 재사용을 거절한다")
    void successfulConfirmationCreatesTenMinuteProofForSameEmailAndCannotBeReplayed() throws Exception {
        String email = email(); String id = id(); String token = UUID.randomUUID().toString();
        store.createVerification(email, "123456", id, id);
        Instant before = Instant.now();
        Instant expires = store.updateVerificationConfirmed(id, "123456", token);
        assertThat(expires).isBetween(before.plusSeconds(598), Instant.now().plusSeconds(601));
        String proofKey = "email-verification:proof:" + hash(CODE_KEY, "proof:" + token);
        assertThat(redis.opsForHash().get(proofKey, "email")).isEqualTo(hash(EMAIL_KEY, email));
        assertThat(redis.opsForHash().get(proofKey, "purpose")).isEqualTo("SIGNUP");
        assertThat(redis.getExpire(proofKey)).isBetween(598L, 600L);
        assertThat(redis.hasKey("email-verification:challenge:" + id)).isFalse();
        expect(ErrorCode.NOT_FOUND, () -> store.updateVerificationConfirmed(id, "123456",
            UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("인증번호가 다섯 번 틀리면 요청을 무효화하고 증명을 발급하지 않는다")
    void fifthWrongCodeInvalidatesChallengeAndNoProofIsCreated() {
        String id = id(); String token = UUID.randomUUID().toString();
        store.createVerification(email(), "123456", id, id);
        for (int i = 0; i < 5; i++) expect(ErrorCode.INVALID_VERIFICATION_CODE, () ->
            store.updateVerificationConfirmed(id, "999999", token));
        expect(ErrorCode.NOT_FOUND, () -> store.updateVerificationConfirmed(id, "123456", token));
        assertThat(redis.hasKey("email-verification:challenge:" + id)).isFalse();
    }

    @Test
    @DisplayName("만료된 요청과 없는 요청을 구분한다")
    void expiredAndUnknownIdsAreDistinguished() {
        String id = id(); store.createVerification(email(), "123456", id, id);
        redis.delete("email-verification:challenge:" + id);
        expect(ErrorCode.VERIFICATION_EXPIRED, () -> store.updateVerificationConfirmed(id, "123456",
            UUID.randomUUID().toString()));
        expect(ErrorCode.NOT_FOUND, () -> store.updateVerificationConfirmed(id(), "123456",
            UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("재발송 후에도 실패 횟수를 유지하고 한 시간 동안 인증을 잠근다")
    void cumulativeFailuresSurviveResendAndLockConfirmationForOneHour() throws Exception {
        String email = email(); String first = id(); String second = id();
        String token = UUID.randomUUID().toString(); String ip = id();
        store.createVerification(email, "123456", first, ip);
        for (int i = 0; i < 5; i++) expect(ErrorCode.INVALID_VERIFICATION_CODE, () ->
            store.updateVerificationConfirmed(first, "999999", token));
        redis.delete("email-verification:cooldown:" + hash(EMAIL_KEY, email));
        store.createVerification(email, "654321", second, ip);
        for (int i = 0; i < 4; i++) expect(ErrorCode.INVALID_VERIFICATION_CODE, () ->
            store.updateVerificationConfirmed(second, "999999", token));
        expect(ErrorCode.RATE_LIMITED, () -> store.updateVerificationConfirmed(second, "999999", token));
        String third = id();
        redis.delete("email-verification:cooldown:" + hash(EMAIL_KEY, email));
        store.createVerification(email, "222222", third, ip);
        String lockKey = "email-verification:confirm-lock:" + hash(EMAIL_KEY, email);
        assertThat(redis.getExpire(lockKey)).isBetween(3598L, 3600L);
        expect(ErrorCode.RATE_LIMITED, () -> store.updateVerificationConfirmed(third, "222222",
            UUID.randomUUID().toString()));
        redis.expire(lockKey, java.time.Duration.ofMillis(20));
        Thread.sleep(50);
        assertThat(store.updateVerificationConfirmed(third, "222222",
            UUID.randomUUID().toString())).isAfter(Instant.now());
    }

    @Test
    @DisplayName("동시 인증 성공 요청에는 증명을 한 번만 발급한다")
    void concurrentCorrectConfirmationsIssueOnlyOneProof() throws Exception {
        String id = id(); store.createVerification(email(), "123456", id, id);
        var executor = Executors.newFixedThreadPool(10);
        try {
            var tasks = IntStream.range(0, 10).<Callable<Boolean>>mapToObj(i -> () -> {
                try { store.updateVerificationConfirmed(id, "123456", UUID.randomUUID().toString()); return true; }
                catch (BusinessException e) { if (e.getErrorCode() != ErrorCode.NOT_FOUND) throw e; return false; }
            }).toList();
            var results = executor.invokeAll(tasks);
            int successes = 0;
            for (var result : results) if (result.get()) successes++;
            assertThat(successes).isEqualTo(1);
        } finally { executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS); }
    }
}
