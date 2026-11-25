package com.louiekury.auth.global;

import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.account.application.signup.*;
import com.louiekury.auth.emailverification.application.port.out.SignupProofVerifier;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignupServiceContractTest {
    @Test
    void databaseFailureConsumesProofAndRequiresNewVerification() {
        var store = mock(SignupStore.class);
        var passwords = mock(EncodePasswordPort.class);
        var available = new AtomicBoolean(true);
        SignupProofVerifier proof = (email, token) -> available.getAndSet(false);
        var service = new SignupService(store, proof, passwords);
        var command = new SignupCommand("Tester", "01012345678", "MALE", LocalDate.of(1995, 1, 1), "Seoul", "nick", "login", "password", "test@example.com", "proof");
        when(store.create(any(), any(), any(), any())).thenThrow(new DataAccessResourceFailureException("test failure"));
        when(passwords.encode(anyString())).thenReturn("hash");
        assertThatThrownBy(() -> service.signup(command)).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SIGNUP_PENDING));
        assertThatThrownBy(() -> service.signup(command)).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL_VERIFICATION));
        verify(store, times(1)).create(any(), any(), any(), any());
    }
}
