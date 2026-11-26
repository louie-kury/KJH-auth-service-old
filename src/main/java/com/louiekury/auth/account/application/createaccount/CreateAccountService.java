package com.louiekury.auth.account.application.createaccount;

import com.louiekury.auth.account.application.port.out.CreateAccountPort;
import com.louiekury.auth.account.application.port.out.CreatePasswordHashPort;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAccountService implements CreateAccountUseCase {

    private final CreateAccountPort createAccountPort;
    private final CreatePasswordHashPort createPasswordHashPort;


    @Override
    @Transactional
    public String createAccount(CreateAccountCommand command) {
        Objects.requireNonNull(command, "command");
        requireText(command.getUuid(), "uuid");
        requireText(command.getStatus(), "status");
        requireText(command.getName(), "name");
        requireText(command.getPhoneNumber(), "phoneNumber");
        requireText(command.getGender(), "gender");
        Objects.requireNonNull(command.getBirthDate(), "birthDate");
        requireText(command.getAddress(), "address");
        requireText(command.getEmail(), "email");
        requireText(command.getLoginId(), "loginId");
        requirePasswordLength(command.getPassword());

        String passwordHash = createPasswordHashPort.createPasswordHash(command.getPassword());
        return createAccountPort.createAccount(CreateAccountData.of(
                command.getUuid(), command.getStatus(), command.getName(), command.getPhoneNumber(),
                command.getGender(), command.getBirthDate(), command.getAddress(), command.getEmail().strip(),
                command.isTermsAgreed(), command.getLoginId(), passwordHash
        ));
    }

    private static void requirePasswordLength(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }
        int length = password.codePointCount(0, password.length());
        if (length < 8 || length > 128) {
            throw new IllegalArgumentException("password must contain 8 to 128 characters");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
