package com.louiekury.auth.emailverification.application.updateemailverification;

import com.louiekury.auth.emailverification.adapter.out.redis.RedisVerificationAdapter;
import com.louiekury.auth.emailverification.application.port.out.UpdateVerificationConfirmationPort;
import com.louiekury.auth.emailverification.application.updateemailverification.UpdateEmailVerificationService;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConfirmVerificationContractTest {
    private final UpdateVerificationConfirmationPort store = mock(UpdateVerificationConfirmationPort.class);
    private final UpdateEmailVerificationService service = new UpdateEmailVerificationService(store);

    @Test
    @DisplayName("정상 인증번호는 고유한 URL 안전 증명과 저장 만료시각을 반환한다")
    void correctCodeProducesUniqueUrlSafeProofAndStoreExpiry() {
        Instant expiry = Instant.parse("2026-11-24T08:00:00Z");
        when(store.updateVerificationConfirmed(eq("verify_123"), eq("001234"), anyString())).thenReturn(expiry);
        var first = service.updateEmailVerificationConfirmed("verify_123", "001234");
        var second = service.updateEmailVerificationConfirmed("verify_123", "001234");
        assertThat(first.getEmailVerificationToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(second.getEmailVerificationToken()).isNotEqualTo(first.getEmailVerificationToken());
        assertThat(first.getExpiresAt()).isEqualTo(expiry);
        verify(store).updateVerificationConfirmed("verify_123", "001234", first.getEmailVerificationToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "12a456", "１２３４５６", "123 56"})
    @DisplayName("잘못된 인증번호는 저장소 접근 전에 거절한다")
    void invalidCodesAreRejectedWithoutAccessingStore(String code) {
        assertThatThrownBy(() -> service.updateEmailVerificationConfirmed("verify_123",
            code)).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("저장소 장애 시 인증 증명을 반환하지 않는다")
    void storeFailureDoesNotReturnProof() {
        when(store.updateVerificationConfirmed(anyString(), anyString(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.VERIFICATION_UNAVAILABLE));
        assertThatThrownBy(() -> service.updateEmailVerificationConfirmed("verify_123",
            "001234")).isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_UNAVAILABLE);
    }

    @Test
    @DisplayName("Redis 장애를 인증 성공으로 처리하지 않는다")
    void unavailableRedisIsNotReportedAsSuccessfulConfirmation() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), anyList(), any(Object[].class))).thenThrow(new IllegalStateException("unavailable"));
        var redisStore = new RedisVerificationAdapter(redis, "email-key-012345678901234567890123456789",
            "code-key-0123456789012345678901234567890", 100, 3600);
        assertThatThrownBy(() -> redisStore.updateVerificationConfirmed("verify_123", "001234", "token"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VERIFICATION_UNAVAILABLE);
    }
}
