package com.louiekury.auth.global;

import com.louiekury.auth.account.application.createaccount.CreateAccountCommand;
import com.louiekury.auth.account.application.createaccount.CreateAccountService;
import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.account.application.port.out.SaveAccountPort;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateAccountServiceContractTest {
    private final SaveAccountPort storage = mock(SaveAccountPort.class);
    private final EncodePasswordPort encoder = mock(EncodePasswordPort.class);
    private final CreateAccountService service = new CreateAccountService(storage, encoder);

    @Test
    void encodesPasswordBeforeSendingAccountToStorage() {
        when(encoder.encode("TestPassword1!!")).thenReturn("encoded-value");
        when(storage.save(any())).thenReturn("test-uuid");
        assertThat(service.create(command("test-login", "TestPassword1!!"))).isEqualTo("test-uuid");
        var saved = ArgumentCaptor.forClass(SaveAccountPort.AccountData.class);
        verify(storage).save(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("encoded-value");
        assertThat(saved.getValue().loginId()).isEqualTo("test-login");
        assertThat(saved.getValue().email()).isEqualTo("User@EXAMPLE.test");
        assertThat(command("test-login", "TestPassword1!!").toString()).doesNotContain("TestPassword1!!");
    }

    @Test
    void rejectsBlankLoginOrPasswordBeforeEncodingOrSaving() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(command("  ", "TestPassword1!!")));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(command("test-login", "  ")));
        assertThatNullPointerException().isThrownBy(() -> service.create(null));
        verifyNoInteractions(storage, encoder);
    }


    @Test
    void countsUnicodeCodePointsAndPreservesPasswordSpaces() {
        String minimum = "😀".repeat(8);
        String unicodeMaximum = "😀".repeat(128);
        String maximum = " ".repeat(128);
        when(encoder.encode(anyString())).thenReturn("encoded-value");
        when(storage.save(any())).thenReturn("test-uuid");
        assertThat(service.create(command("test-login", minimum))).isEqualTo("test-uuid");
        assertThat(service.create(command("test-login", maximum))).isEqualTo("test-uuid");
        assertThat(service.create(command("test-login", unicodeMaximum))).isEqualTo("test-uuid");
        verify(encoder).encode(minimum);
        verify(encoder).encode(unicodeMaximum);
        verify(encoder).encode(maximum);
    }

    @Test
    void rejectsTooShortAndTooLongUnicodePasswordsBeforeSaving() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(command("test-login", "😀".repeat(7))));
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(command("test-login", "😀".repeat(129))));
        verifyNoInteractions(storage, encoder);
    }
    private static CreateAccountCommand command(String loginId, String password) {
        return new CreateAccountCommand("test-uuid", "PENDING", "test", "01012345678", "FEMALE", LocalDate.of(1990, 1, 1), "test address", "  User@EXAMPLE.test  ", true, loginId, password);
    }
}