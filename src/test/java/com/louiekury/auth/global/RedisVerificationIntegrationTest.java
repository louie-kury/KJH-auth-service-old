package com.louiekury.auth.global;

import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_PORT", matches = "[0-9]+")
class RedisVerificationIntegrationTest {
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static RedisVerificationStore store;

    @BeforeAll
    static void connectToIsolatedRedis() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("TEST_REDIS_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        store = new RedisVerificationStore(redis, "email-key-012345678901234567890123456789", "code-key-0123456789012345678901234567890", 100, 3600);
    }

    @AfterAll
    static void disconnect() { if (factory != null) factory.destroy(); }

    @Test
    void cooldownAndChallengeUseExpectedTtls() {
        String email = UUID.randomUUID() + "@example.com";
        String id = UUID.randomUUID().toString();
        Instant before = Instant.now();
        Instant expires = store.reserve(email, "000123", id, "192.0.2.1");
        assertThat(expires).isBetween(before.plusSeconds(298), Instant.now().plusSeconds(301));
        assertThat(redis.getExpire("email-verification:challenge:" + id)).isBetween(298L, 300L);
        assertThatThrownBy(() -> store.reserve(email, "000456", UUID.randomUUID().toString(), "192.0.2.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
    }

    private static String emailHash(String email) throws Exception {
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
            "email-key-012345678901234567890123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(email.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void resendIsAcceptedWhenCooldownExpiresAndInvalidatesEarlierChallenge() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        store.reserve(email, "000123", first, "cooldown-" + first);
        String cooldown = "email-verification:cooldown:" + emailHash(email);
        assertThat(redis.getExpire(cooldown)).isBetween(59L, 60L);
        assertThatThrownBy(() -> store.reserve(email, "000456", second, "cooldown-" + first))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
        redis.expire(cooldown, java.time.Duration.ofMillis(20));
        Thread.sleep(50);
        store.reserve(email, "000456", second, "cooldown-" + first);
        assertThat(redis.hasKey("email-verification:challenge:" + first)).isFalse();
        assertThat(redis.hasKey("email-verification:challenge:" + second)).isTrue();
    }

    @Test
    void hourlyEmailLimitAllowsAgainAfterOldestRequestExpires() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String hash = emailHash(email);
        String cooldown = "email-verification:cooldown:" + hash;
        String rate = "email-verification:rate:" + hash;
        String ip = "hourly-" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            store.reserve(email, "123456", UUID.randomUUID().toString(), ip);
            redis.delete(cooldown); // Advance past each cooldown without changing the hourly history.
        }
        assertThatThrownBy(() -> store.reserve(email, "123456", UUID.randomUUID().toString(), ip))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
        var oldest = redis.opsForZSet().range(rate, 0, 0).iterator().next();
        long now = Instant.now().getEpochSecond();
        redis.opsForZSet().add(rate, oldest, now - 3590);
        assertThatThrownBy(() -> store.reserve(email, "123456", UUID.randomUUID().toString(), ip))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
        redis.opsForZSet().add(rate, oldest, now - 3601);
        store.reserve(email, "123456", UUID.randomUUID().toString(), ip);
        assertThat(redis.opsForZSet().size(rate)).isEqualTo(5);
    }
    @Test
    void ipLimitRejectsTheOneHundredAndFirstRequest() {
        String ip = "ip-test-" + UUID.randomUUID();
        for (int i = 0; i < 100; i++) store.reserve(UUID.randomUUID() + "@example.com", "123456", UUID.randomUUID().toString(), ip);
        assertThatThrownBy(() -> store.reserve(UUID.randomUUID() + "@example.com", "123456", UUID.randomUUID().toString(), ip))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED);
    }

    @Test
    void concurrentRequestsForSameAddressReserveOnlyOnce() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var executor = Executors.newFixedThreadPool(10);
        try {
            var tasks = IntStream.range(0, 10).<Callable<Boolean>>mapToObj(i -> () -> {
                try { store.reserve(email, "123456", UUID.randomUUID().toString(), "192.0.2.2"); return true; }
                catch (BusinessException e) { if (e.getErrorCode() != ErrorCode.RATE_LIMITED) throw e; return false; }
            }).toList();
            var results = executor.invokeAll(tasks);
            long accepted = 0;
            for (var result : results) if (result.get()) accepted++;
            assertThat(accepted).isEqualTo(1);
        } finally { executor.shutdownNow(); executor.awaitTermination(5, TimeUnit.SECONDS); }
    }
}
