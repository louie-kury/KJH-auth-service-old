package com.louiekury.auth.account.application.login;

import com.louiekury.auth.account.adapter.out.security.LoginPasswordAdapter;
import com.louiekury.auth.account.adapter.out.security.PasswordEncodingConfig;
import com.louiekury.auth.account.application.port.out.CreateLoginTokenPort;
import com.louiekury.auth.account.application.port.out.GetLoginAccountPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateLoginServiceTest {
    private final GetLoginAccountPort accounts = mock(GetLoginAccountPort.class);
    private final PasswordEncoder encoder = new PasswordEncodingConfig().passwordEncoder();
    private final CreateLoginTokenPort tokens = mock(CreateLoginTokenPort.class);
    private final CreateLoginService service = new CreateLoginService(accounts, new LoginPasswordAdapter(encoder), tokens);
    private final String hash = encoder.encode("correct-password");

    @Test
    @DisplayName("정상 계정 로그인 시 토큰을 발급한다")
    void validAccountGetsToken() {
        when(accounts.getLoginAccount("user")).thenReturn(Optional.of(GetLoginAccountResult.of("member-id", hash, true)));
        service.createLogin(CreateLoginCommand.of("user", "correct-password"));
        verify(tokens).createLoginToken("member-id");
    }

    @Test
    @DisplayName("없는 아이디와 잘못된 비밀번호는 동일한 오류를 반환한다")
    void unknownAndWrongPasswordHaveSameErrorAndNoToken() {
        when(accounts.getLoginAccount("unknown")).thenReturn(Optional.empty());
        when(accounts.getLoginAccount("user")).thenReturn(Optional.of(GetLoginAccountResult.of("member-id", hash, true)));
        assertThatThrownBy(() -> service.createLogin(CreateLoginCommand.of("unknown", "wrong-password")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThatThrownBy(() -> service.createLogin(CreateLoginCommand.of("user", "wrong-password")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verifyNoInteractions(tokens);
    }

    @Test
    @DisplayName("사용할 수 없는 계정에는 토큰을 발급하지 않는다")
    void unavailableAccountDoesNotReceiveToken() {
        when(accounts.getLoginAccount("user")).thenReturn(Optional.of(GetLoginAccountResult.of("member-id", hash, false)));
        assertThatThrownBy(() -> service.createLogin(CreateLoginCommand.of("user", "correct-password")))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_UNAVAILABLE);
        verifyNoInteractions(tokens);
    }

    @Test
    @DisplayName("손상되거나 없는 비밀번호 해시로 로그인할 수 없다")
    void damagedAndNullHashesDoNotGrantAccess() {
        for (String invalidHash : new String[]{null, "damaged-hash"}) {
            when(accounts.getLoginAccount("user")).thenReturn(Optional.of(GetLoginAccountResult.of("member-id", invalidHash, true)));
            assertThatThrownBy(() -> service.createLogin(CreateLoginCommand.of("user", "correct-password")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
        verifyNoInteractions(tokens);
    }
}