package com.louiekury.auth.emailverification.adapter.out.redis;

import com.louiekury.auth.emailverification.application.port.out.VerificationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisVerificationStore implements VerificationStore {
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
        local now = tonumber(redis.call('TIME')[1])
        redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - 3600)
        redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', now - tonumber(ARGV[5]))
        if redis.call('EXISTS', KEYS[2]) == 1 or redis.call('ZCARD', KEYS[1]) >= 5 or redis.call('ZCARD', KEYS[3]) >= tonumber(ARGV[4]) then return 0 end
        local old = redis.call('GET', KEYS[4])
        if old then redis.call('DEL', ARGV[6] .. old) end
        redis.call('SET', KEYS[2], '1', 'EX', 60)
        redis.call('ZADD', KEYS[1], now, ARGV[1]); redis.call('EXPIRE', KEYS[1], 3600)
        redis.call('ZADD', KEYS[3], now, ARGV[1]); redis.call('EXPIRE', KEYS[3], tonumber(ARGV[5]))
        redis.call('SET', KEYS[4], ARGV[1], 'EX', 300)
        redis.call('HSET', KEYS[5], 'email', ARGV[2], 'code', ARGV[3], 'purpose', 'SIGNUP', 'expiresAt', now + 300, 'failures', '0')
        redis.call('EXPIRE', KEYS[5], 300)
        return now + 300
        """, Long.class);
    private static final String PREFIX = "email-verification:challenge:";
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
        if (emailKey.getBytes(StandardCharsets.UTF_8).length < 32 || codeKey.getBytes(StandardCharsets.UTF_8).length < 32
                || emailKey.equals(codeKey) || ipLimit <= 0 || ipWindowSeconds <= 0) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
        String emailHash = hmac(emailKey, email);
        String ipHash = hmac(emailKey, "ip:" + clientIp);
        String codeHash = hmac(codeKey, id + ":" + code);
        try {
            Long expires = redis.execute(RESERVE, List.of("email-verification:rate:" + emailHash,
                "email-verification:cooldown:" + emailHash, "email-verification:ip:" + ipHash,
                "email-verification:latest:" + emailHash, PREFIX + id), id, emailHash, codeHash,
                Integer.toString(ipLimit), Integer.toString(ipWindowSeconds), PREFIX);
            if (expires == null) throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
            if (expires == 0) throw new BusinessException(ErrorCode.RATE_LIMITED);
            return Instant.ofEpochSecond(expires);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
    }

    private String hmac(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
    }
}
