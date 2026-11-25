package com.louiekury.auth.global;

import com.louiekury.auth.emailverification.application.port.out.RegisteredEmailLookup;
import com.louiekury.auth.emailverification.application.port.out.VerificationSender;
import com.louiekury.auth.emailverification.application.port.out.VerificationStore;
import com.louiekury.auth.emailverification.application.send.SendVerificationService;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendVerificationContractTest {
    private final RegisteredEmailLookup lookup = mock(RegisteredEmailLookup.class);
    private final VerificationStore store = mock(VerificationStore.class);
    private final VerificationSender sender = mock(VerificationSender.class);
    private final SendVerificationService service = new SendVerificationService(lookup, store, sender);

    @Test
    void normalizesTheEntireEmailWithoutDiscardingPlusOrDotsAndSendsSixDigits() {
        Instant expires = Instant.parse("2026-11-24T06:05:00Z");
        when(store.reserve(anyString(), anyString(), anyString(), anyString())).thenReturn(expires);
        var result = service.send("  User.Name+Tag@EXAMPLE.com  ", "SIGNUP", "127.0.0.1");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(lookup).exists("user.name+tag@example.com");
        verify(store).reserve(eq("user.name+tag@example.com"), code.capture(), eq(result.verificationId()), eq("127.0.0.1"));
        assertThat(code.getValue()).matches("[0-9]{6}");
        verify(sender).send("User.Name+Tag@EXAMPLE.com", code.getValue());
        assertThat(result.expiresAt()).isEqualTo(expires);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "café@example.com", "user@예시.com", "bad..dots@example.com", "test@example", "@example.com"})
    void rejectsInvalidOrNonAsciiEmailWithoutSending(String email) {
        assertThatThrownBy(() -> service.send(email, "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(lookup, store, sender);
    }

    @Test
    void rejectsRegisteredEmailBeforeReservingOrSending() {
        when(lookup.exists("user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.send("User@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.valueOf("EMAIL_ALREADY_EXISTS"));
        verifyNoInteractions(store, sender);
    }

    @Test
    void storeFailureNeverAttemptsMail() {
        when(store.reserve(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.RATE_LIMITED));
        assertThatThrownBy(() -> service.send("user@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.RATE_LIMITED);
        verifyNoInteractions(sender);
    }

    @Test
    void mailFailureIsNotReportedAsSuccess() {
        when(store.reserve(anyString(), anyString(), anyString(), anyString())).thenReturn(Instant.now().plusSeconds(300));
        doThrow(new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED)).when(sender).send(anyString(), anyString());
        assertThatThrownBy(() -> service.send("user@example.com", "SIGNUP", "127.0.0.1"))
            .isInstanceOf(BusinessException.class).extracting(e -> ((BusinessException) e).getErrorCode())
            .isEqualTo(ErrorCode.VERIFICATION_DELIVERY_FAILED);
    }
}
