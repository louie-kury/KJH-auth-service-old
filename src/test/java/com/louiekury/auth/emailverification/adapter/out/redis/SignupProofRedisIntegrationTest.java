package com.louiekury.auth.emailverification.adapter.out.redis;

import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationAdapter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_PORT", matches = "[0-9]+")
class SignupProofRedisIntegrationTest {
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static RedisVerificationAdapter store;

    @BeforeAll
    static void setup() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("TEST_REDIS_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        store = new RedisVerificationAdapter(redis, "email-key-012345678901234567890123456789",
            "code-key-0123456789012345678901234567890", 100, 3600);
    }

    @AfterAll
    static void cleanup() { if (factory != null) factory.destroy(); }

    private String issue(String email, String token) throws Exception {
        String id = UUID.randomUUID().toString();
        store.createVerification(email, "123456", id, "ip-" + id);
        store.updateVerificationConfirmed(id, "123456", token);
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new
            javax.crypto.spec.SecretKeySpec(
                "code-key-0123456789012345678901234567890".getBytes(java.nio.charset.StandardCharsets.UTF_8),
             "HmacSHA256"));
        return "email-verification:proof:" + java.util.HexFormat.of().formatHex(mac.doFinal(("proof:" +
            token).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("인증 증명의 이메일을 확인하고 한 번만 사용한다")
    void proofChecksEmailAndIsConsumedOnce() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String token = UUID.randomUUID().toString() + "abcdefg";
        String key = issue(email, token);
        try {
            assertThat(store.updateSignupProofConsumed("other@example.com", token)).isFalse();
            assertThat(store.updateSignupProofConsumed(email.toUpperCase(), token)).isTrue();
            assertThat(store.updateSignupProofConsumed(email, token)).isFalse();
        } finally { redis.delete(key); }
    }

    @Test
    @DisplayName("가입 목적이 아니거나 만료된 인증 증명을 거절한다")
    void nonSignupPurposeAndExpiredProofAreRejected() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String token = UUID.randomUUID().toString() + "abcdefg";
        String key = issue(email, token);
        try {
            redis.opsForHash().put(key, "purpose", "EMAIL_CHANGE");
            assertThat(store.updateSignupProofConsumed(email, token)).isFalse();
            redis.opsForHash().put(key, "purpose", "SIGNUP");
            redis.opsForHash().put(key, "expiresAt", Long.toString(Instant.now().minusSeconds(1).getEpochSecond()));
            assertThat(store.updateSignupProofConsumed(email, token)).isFalse();
        } finally { redis.delete(key); }
    }

    @Test
    @DisplayName("동시 인증 증명 사용은 한 번만 성공한다")
    void simultaneousConsumptionSucceedsExactlyOnce() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String token = UUID.randomUUID().toString() + "abcdefg";
        String key = issue(email, token);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = pool.submit(() -> { start.await(); return store.updateSignupProofConsumed(email, token); });
            var b = pool.submit(() -> { start.await(); return store.updateSignupProofConsumed(email, token); });
            start.countDown();
            assertThat(java.util.List.of(a.get(), b.get())).containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); redis.delete(key); }
    }
}
