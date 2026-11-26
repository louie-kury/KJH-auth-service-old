package com.louiekury.auth.account.application.createsignup;

import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import com.louiekury.auth.account.application.port.out.CreateSignupPort;
import com.louiekury.auth.account.application.port.out.UpdateSignupProofStatusPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignupServiceContractTest {
    @Test
    @DisplayName("DB 저장이 실패해도 증명을 소모하고 재인증을 요구한다")
    void databaseFailureConsumesProofAndRequiresNewVerification() {
        var store = mock(CreateSignupPort.class);
        var passwords = mock(CreatePasswordHashPort.class);
        var available = new AtomicBoolean(true);
        UpdateSignupProofStatusPort proof = (email, token) -> available.getAndSet(false);
        var service = new CreateSignupService(store, proof, passwords);
        var command = CreateSignupCommand.of("Tester", "01012345678", "MALE", LocalDate.of(1995, 1, 1), "Seoul", "nick",
             "login", "password", "test@example.com", "proof");
        when(store.createSignup(any(), any(), any(), any())).thenThrow(new CreateSignupPort.StorageException(new
            DataAccessResourceFailureException("test failure")));
        when(passwords.createPasswordHash(anyString())).thenReturn("hash");
        assertThatThrownBy(() -> service.createSignup(command)).isInstanceOfSatisfying(BusinessException.class, e ->
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SIGNUP_PENDING));
        assertThatThrownBy(() -> service.createSignup(command)).isInstanceOfSatisfying(BusinessException.class, e ->
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_VERIFICATION));
        verify(store, times(1)).createSignup(any(), any(), any(), any());
    }
}
