package com.louiekury.auth.global;

import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationService;
import com.louiekury.auth.emailverification.application.port.out.VerificationConfirmationStore;
import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationStore;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfirmVerificationContractTest {
    private final VerificationConfirmationStore store = mock(VerificationConfirmationStore.class);
    private final ConfirmVerificationService service = new ConfirmVerificationService(store);

    @Test
    void correctCodeProducesUniqueUrlSafeProofAndStoreExpiry() {
        Instant expiry = Instant.parse("2026-11-24T08:00:00Z");
        when(store.confirm(eq("verify_123"), eq("001234"), anyString())).thenReturn(expiry);
        var first = service.confirm("verify_123", "001234");
        var second = service.confirm("verify_123", "001234");
        assertThat(first.emailVerificationToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(second.emailVerificationToken()).isNotEqualTo(first.emailVerificationToken());
        assertThat(first.expiresAt()).isEqualTo(expiry);
        verify(store).confirm("verify_123", "001234", first.emailVerificationToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "12a456", "１２３４５６", "123 56"})
    void invalidCodesAreRejectedWithoutAccessingStore(String code) {
        assertThatThrownBy(() -> service.confirm("verify_123", code)).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(store);
    }

    @Test
    void storeFailureDoesNotReturnProof() {
        when(store.confirm(anyString(), anyString(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.VERIFICATION_UNAVAILABLE));
        assertThatThrownBy(() -> service.confirm("verify_123", "001234")).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_UNAVAILABLE);
    }

    @Test
    void unavailableRedisIsNotReportedAsSuccessfulConfirmation() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), anyList(), any(Object[].class))).thenThrow(new IllegalStateException("unavailable"));
        var redisStore = new RedisVerificationStore(redis, "email-key-012345678901234567890123456789",
            "code-key-0123456789012345678901234567890", 100, 3600);
        assertThatThrownBy(() -> redisStore.confirm("verify_123", "001234", "token"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VERIFICATION_UNAVAILABLE);
    }
}
