package com.louiekury.auth.emailverification.adapter.out.redis;

import com.louiekury.auth.emailverification.application.port.out.VerificationConfirmationStore;
import com.louiekury.auth.emailverification.application.port.out.VerificationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisVerificationStore implements VerificationStore, VerificationConfirmationStore {
    private static final String CHALLENGE = "email-verification:challenge:";
    private static final String MARKER = "email-verification:marker:";
    private static final String PROOF = "email-verification:proof:";
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
        local now = tonumber(redis.call('TIME')[1])
        redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - 3600)
        redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', now - tonumber(ARGV[5]))
        if redis.call('EXISTS', KEYS[2]) == 1 or redis.call('ZCARD', KEYS[1]) >= 5 or redis.call('ZCARD', KEYS[3]) >= tonumber(ARGV[4]) then return 0 end
        local old = redis.call('GET', KEYS[4])
        if old then
            redis.call('DEL', ARGV[6] .. old)
            local oldmarker = ARGV[7] .. old
            if redis.call('EXISTS', oldmarker) == 1 then redis.call('SET', oldmarker, 'revoked', 'KEEPTTL') end
        end
        redis.call('SET', KEYS[2], '1', 'EX', 60)
        redis.call('ZADD', KEYS[1], now, ARGV[1]); redis.call('EXPIRE', KEYS[1], 3600)
        redis.call('ZADD', KEYS[3], now, ARGV[1]); redis.call('EXPIRE', KEYS[3], tonumber(ARGV[5]))
        redis.call('SET', KEYS[4], ARGV[1], 'EX', 300)
        redis.call('HSET', KEYS[5], 'email', ARGV[2], 'code', ARGV[3], 'purpose', 'SIGNUP', 'expiresAt', now + 300, 'failures', '0')
        redis.call('EXPIRE', KEYS[5], 300)
        redis.call('SET', KEYS[6], 'active', 'EX', 86400)
        return now + 300
        """, Long.class);
    private static final DefaultRedisScript<Long> CONFIRM = new DefaultRedisScript<>("""
        local now = tonumber(redis.call('TIME')[1])
        if redis.call('EXISTS', KEYS[1]) == 0 then
            if redis.call('GET', KEYS[2]) == 'active' then return -2 end
            return -1
        end
        local email = redis.call('HGET', KEYS[1], 'email')
        local code = redis.call('HGET', KEYS[1], 'code')
        local purpose = redis.call('HGET', KEYS[1], 'purpose')
        local expires = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
        if not email or not code or purpose ~= 'SIGNUP' or not expires then return -5 end
        if expires <= now then
            redis.call('DEL', KEYS[1])
            return -2
        end
        local lock = 'email-verification:confirm-lock:' .. email
        if redis.call('EXISTS', lock) == 1 then return -3 end
        local failures = 'email-verification:confirm-failures:' .. email
        redis.call('ZREMRANGEBYSCORE', failures, '-inf', now - 3600)
        if code ~= ARGV[1] then
            local count = redis.call('HINCRBY', KEYS[1], 'failures', 1)
            redis.call('ZADD', failures, now, ARGV[2]); redis.call('EXPIRE', failures, 3600)
            if count >= 5 then
                redis.call('DEL', KEYS[1])
                if redis.call('EXISTS', KEYS[2]) == 1 then redis.call('SET', KEYS[2], 'revoked', 'KEEPTTL') end
            end
            if redis.call('ZCARD', failures) >= 10 then
                redis.call('SET', lock, '1', 'EX', 3600)
                return -3
            end
            return -4
        end
        if redis.call('EXISTS', KEYS[3]) == 1 then return -5 end
        redis.call('HSET', KEYS[3], 'email', email, 'purpose', 'SIGNUP', 'expiresAt', now + 600)
        redis.call('EXPIRE', KEYS[3], 600)
        redis.call('DEL', KEYS[1])
        if redis.call('EXISTS', KEYS[2]) == 1 then redis.call('SET', KEYS[2], 'used', 'KEEPTTL') end
        return now + 600
        """, Long.class);
    private final StringRedisTemplate redis;
    private final String emailKey;
    private final String codeKey;
    private final int ipLimit;
    private final int ipWindowSeconds;

    public RedisVerificationStore(StringRedisTemplate redis,
            @Value("${verification.email-hmac-key:}") String emailKey,
            @Value("${verification.code-hmac-key:}") String codeKey,
            @Value("${verification.ip-limit:100}") int ipLimit,
            @Value("${verification.ip-window-seconds:3600}") int ipWindowSeconds) {
        this.redis = redis;
        this.emailKey = emailKey;
        this.codeKey = codeKey;
        this.ipLimit = ipLimit;
        this.ipWindowSeconds = ipWindowSeconds;
    }

    public Instant reserve(String email, String code, String id, String clientIp) {
        validateKeys(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        if (ipLimit <= 0 || ipWindowSeconds <= 0) throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        String emailHash = hmac(emailKey, email);
        String ipHash = hmac(emailKey, "ip:" + clientIp);
        String codeHash = hmac(codeKey, id + ":" + code);
        try {
            Long expires = redis.execute(RESERVE, List.of("email-verification:rate:" + emailHash,
                "email-verification:cooldown:" + emailHash, "email-verification:ip:" + ipHash,
                "email-verification:latest:" + emailHash, CHALLENGE + id, MARKER + id), id, emailHash, codeHash,
                Integer.toString(ipLimit), Integer.toString(ipWindowSeconds), CHALLENGE, MARKER);
            if (expires == null) throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
            if (expires == 0) throw new BusinessException(ErrorCode.RATE_LIMITED);
            return Instant.ofEpochSecond(expires);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
    }

    public Instant confirm(String id, String code, String token) {
        validateKeys(ErrorCode.VERIFICATION_UNAVAILABLE);
        String codeHash = hmac(codeKey, id + ":" + code);
        String proofHash = hmac(codeKey, "proof:" + token);
        try {
            Long result = redis.execute(CONFIRM, List.of(CHALLENGE + id, MARKER + id, PROOF + proofHash),
                codeHash, UUID.randomUUID().toString());
            if (result == null || result == -5) throw new BusinessException(ErrorCode.VERIFICATION_UNAVAILABLE);
            if (result == -1) throw new BusinessException(ErrorCode.NOT_FOUND);
            if (result == -2) throw new BusinessException(ErrorCode.VERIFICATION_EXPIRED);
            if (result == -3) throw new BusinessException(ErrorCode.RATE_LIMITED);
            if (result == -4) throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
            return Instant.ofEpochSecond(result);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_UNAVAILABLE);
        }
    }

    private void validateKeys(ErrorCode errorCode) {
        if (emailKey.getBytes(StandardCharsets.UTF_8).length < 32 || codeKey.getBytes(StandardCharsets.UTF_8).length < 32
                || emailKey.equals(codeKey)) {
            throw new BusinessException(errorCode);
        }
    }

    private String hmac(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_UNAVAILABLE);
        }
    }
}
