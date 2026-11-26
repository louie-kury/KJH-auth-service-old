package com.louiekury.auth.account.application.createaccount;

import com.louiekury.auth.account.application.createaccount.CreateAccountCommand;
import com.louiekury.auth.account.application.createaccount.CreateAccountData;
import com.louiekury.auth.account.application.createaccount.CreateAccountService;
import com.louiekury.auth.account.application.port.out.CreateAccountPort;
import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateAccountServiceContractTest {
    private final CreateAccountPort storage = mock(CreateAccountPort.class);
    private final CreatePasswordHashPort encoder = mock(CreatePasswordHashPort.class);
    private final CreateAccountService service = new CreateAccountService(storage, encoder);

    @Test
    @DisplayName("계정 저장 전에 비밀번호를 인코딩한다")
    void encodesPasswordBeforeSendingAccountToStorage() {
        when(encoder.createPasswordHash("TestPassword1!!")).thenReturn("encoded-value");
        when(storage.createAccount(any())).thenReturn("test-uuid");
        assertThat(service.createAccount(command("test-login", "TestPassword1!!"))).isEqualTo("test-uuid");
        var saved = ArgumentCaptor.forClass(CreateAccountData.class);
        verify(storage).createAccount(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded-value");
        assertThat(saved.getValue().getLoginId()).isEqualTo("test-login");
        assertThat(saved.getValue().getEmail()).isEqualTo("User@EXAMPLE.test");
        assertThat(command("test-login", "TestPassword1!!").toString()).doesNotContain("TestPassword1!!");
    }

    @Test
    @DisplayName("아이디나 비밀번호가 공백이면 인코딩과 저장 전에 거절한다")
    void rejectsBlankLoginOrPasswordBeforeEncodingOrSaving() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.createAccount(command("  ", "TestPassword1!!")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.createAccount(command("test-login", "  ")));
        assertThatNullPointerException().isThrownBy(() -> service.createAccount(null));
        verifyNoInteractions(storage, encoder);
    }


    @Test
    @DisplayName("비밀번호 길이를 코드 포인트로 세고 공백을 보존한다")
    void countsUnicodeCodePointsAndPreservesPasswordSpaces() {
        String minimum = "😀".repeat(8);
        String unicodeMaximum = "😀".repeat(128);
        String maximum = " ".repeat(128);
        when(encoder.createPasswordHash(anyString())).thenReturn("encoded-value");
        when(storage.createAccount(any())).thenReturn("test-uuid");
        assertThat(service.createAccount(command("test-login", minimum))).isEqualTo("test-uuid");
        assertThat(service.createAccount(command("test-login", maximum))).isEqualTo("test-uuid");
        assertThat(service.createAccount(command("test-login", unicodeMaximum))).isEqualTo("test-uuid");
        verify(encoder).createPasswordHash(minimum);
        verify(encoder).createPasswordHash(unicodeMaximum);
        verify(encoder).createPasswordHash(maximum);
    }

    @Test
    @DisplayName("비밀번호가 길이 범위를 벗어나면 저장 전에 거절한다")
    void rejectsTooShortAndTooLongUnicodePasswordsBeforeSaving() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.createAccount(command("test-login",
            "😀".repeat(7))));
        assertThatIllegalArgumentException().isThrownBy(() -> service.createAccount(command("test-login",
            "😀".repeat(129))));
        verifyNoInteractions(storage, encoder);
    }
    private static CreateAccountCommand command(String loginId, String password) {
        return CreateAccountCommand.of("test-uuid", "PENDING", "test", "01012345678", "FEMALE", LocalDate.of(1990, 1,
            1), "test address", "  User@EXAMPLE.test  ", true, loginId, password);
    }
}