package com.louiekury.auth.account.application.createaccount;

import com.louiekury.auth.account.application.port.out.EncodePasswordPort;
import com.louiekury.auth.account.application.port.out.SaveAccountPort;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAccountService implements CreateAccountUseCase {

    private final SaveAccountPort saveAccountPort;
    private final EncodePasswordPort encodePasswordPort;

    public CreateAccountService(SaveAccountPort saveAccountPort, EncodePasswordPort encodePasswordPort) {
        this.saveAccountPort = saveAccountPort;
        this.encodePasswordPort = encodePasswordPort;
    }

    @Override
    @Transactional
    public String create(CreateAccountCommand command) {
        Objects.requireNonNull(command, "command");
        requireText(command.uuid(), "uuid");
        requireText(command.status(), "status");
        requireText(command.name(), "name");
        requireText(command.phoneNumber(), "phoneNumber");
        requireText(command.gender(), "gender");
        Objects.requireNonNull(command.birthDate(), "birthDate");
        requireText(command.address(), "address");
        requireText(command.email(), "email");
        requireText(command.loginId(), "loginId");
        requirePasswordLength(command.password());

        String passwordHash = encodePasswordPort.encode(command.password());
        return saveAccountPort.save(new SaveAccountPort.AccountData(
                command.uuid(), command.status(), command.name(), command.phoneNumber(),
                command.gender(), command.birthDate(), command.address(), command.email().strip(),
                command.termsAgreed(), command.loginId(), passwordHash
        ));
    }

    private static void requirePasswordLength(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }
        int length = password.codePointCount(0, password.length());
        if (length < 15 || length > 128) {
            throw new IllegalArgumentException("password must contain 15 to 128 characters");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
