package com.louiekury.auth.global;

import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationStore;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_PORT", matches = "[0-9]+")
class SignupProofRedisIntegrationTest {
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static RedisVerificationStore store;

    @BeforeAll
    static void setup() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("TEST_REDIS_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        store = new RedisVerificationStore(redis, "email-key-012345678901234567890123456789",
            "code-key-0123456789012345678901234567890", 100, 3600);
    }

    @AfterAll
    static void cleanup() { if (factory != null) factory.destroy(); }

    @Test
    void issuedProofValidatesEmailAndCanBeRecheckedWithoutConsumption() {
        String email = UUID.randomUUID() + "@example.com";
        String id = UUID.randomUUID().toString();
        String token = "A".repeat(43);
        store.reserve(email, "123456", id, "ip-" + id);
        store.confirm(id, "123456", token);
        var proof = store.verifySignupProof(email.toUpperCase(), token);
        assertThat(proof).isPresent();
        assertThat(proof.orElseThrow().expiresAt()).isAfter(Instant.now());
        assertThat(store.verifySignupProof(email, token)).isEqualTo(proof);
        assertThat(store.verifySignupProof("other@example.com", token)).isEmpty();
        assertThat(store.verifySignupProof(email, "B".repeat(43))).isEmpty();
        redis.delete("email-verification:proof:" + proof.orElseThrow().proofDigest());
        assertThat(store.verifySignupProof(email, token)).isEmpty();
    }

    @Test
    void nonSignupPurposeAndExpiredProofAreRejected() {
        String email = UUID.randomUUID() + "@example.com";
        String id = UUID.randomUUID().toString();
        String token = "C".repeat(43);
        store.reserve(email, "123456", id, "ip-" + id);
        store.confirm(id, "123456", token);
        String proofKey = "email-verification:proof:" + store.verifySignupProof(email, token).orElseThrow().proofDigest();
        redis.opsForHash().put(proofKey, "purpose", "EMAIL_CHANGE");
        assertThat(store.verifySignupProof(email, token)).isEmpty();
        redis.opsForHash().put(proofKey, "purpose", "SIGNUP");
        redis.opsForHash().put(proofKey, "expiresAt", Long.toString(Instant.now().minusSeconds(1).getEpochSecond()));
        assertThat(store.verifySignupProof(email, token)).isEmpty();
        redis.delete(proofKey);
    }
}
