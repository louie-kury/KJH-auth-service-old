package com.louiekury.auth.emailverification.application.createemailverification;

import com.louiekury.auth.emailverification.application.createemailverification.CreateEmailVerificationService;
import com.louiekury.auth.emailverification.application.port.out.CreateVerificationDeliveryPort;
import com.louiekury.auth.emailverification.application.port.out.CreateVerificationPort;
import com.louiekury.auth.emailverification.application.port.out.GetRegisteredEmailPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SendVerificationContractTest {
    private final GetRegisteredEmailPort lookup = mock(GetRegisteredEmailPort.class);
    private final CreateVerificationPort store = mock(CreateVerificationPort.class);
    private final CreateVerificationDeliveryPort sender = mock(CreateVerificationDeliveryPort.class);
    private final CreateEmailVerificationService service = new CreateEmailVerificationService(lookup, store, sender);

    @Test
    @DisplayName("이메일의 점과 더하기를 유지해 정규화하고 6자리 인증번호를 보낸다")
    void normalizesTheEntireEmailWithoutDiscardingPlusOrDotsAndSendsSixDigits() {
        Instant expires = Instant.parse("2026-11-24T06:05:00Z");
        when(store.createVerification(anyString(), anyString(), anyString(), anyString())).thenReturn(expires);
        var result = service.createEmailVerification("  User.Name+Tag@EXAMPLE.com  ", "SIGNUP", "127.0.0.1");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(lookup).getRegisteredEmailExists("user.name+tag@example.com");
        verify(store).createVerification(eq("user.name+tag@example.com"), code.capture(),
            eq(result.getVerificationId()), eq("127.0.0.1"));
        assertThat(code.getValue()).matches("[0-9]{6}");
        verify(sender).createVerificationDelivery("User.Name+Tag@EXAMPLE.com", code.getValue());
        assertThat(result.getExpiresAt()).isEqualTo(expires);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "café@example.com", "user@예시.com", "bad..dots@example.com", "test@example",
        "@example.com"})
    @DisplayName("잘못됐거나 ASCII가 아닌 이메일은 발송하지 않는다")
    void rejectsInvalidOrNonAsciiEmailWithoutSending(String email) {
        assertThatThrownBy(() -> service.createEmailVerification(email, "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(lookup, store, sender);
    }

    @Test
    @DisplayName("가입된 이메일은 예약과 발송 전에 거절한다")
    void rejectsRegisteredEmailBeforeReservingOrSending() {
        when(lookup.getRegisteredEmailExists("user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.createEmailVerification("User@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.valueOf("EMAIL_ALREADY_EXISTS"));
        verifyNoInteractions(store, sender);
    }

    @Test
    @DisplayName("인증 저장에 실패하면 메일을 발송하지 않는다")
    void storeFailureNeverAttemptsMail() {
        when(store.createVerification(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.RATE_LIMITED));
        assertThatThrownBy(() -> service.createEmailVerification("user@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.RATE_LIMITED);
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("메일 발송 실패를 성공으로 보고하지 않는다")
    void mailFailureIsNotReportedAsSuccess() {
        when(store.createVerification(anyString(), anyString(), anyString(),
            anyString())).thenReturn(Instant.now().plusSeconds(300));
        doThrow(new
            BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED))
            .when(sender).createVerificationDelivery(anyString(),
             anyString());
        assertThatThrownBy(() -> service.createEmailVerification("user@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VERIFICATION_DELIVERY_FAILED);
    }
}
