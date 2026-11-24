package com.louiekury.auth.account.application.createaccount;

import java.time.LocalDate;

public record CreateAccountCommand(
        String uuid,
        String status,
        String name,
        String phoneNumber,
        String gender,
        LocalDate birthDate,
        String address,
        String email,
        boolean termsAgreed,
        String loginId,
        String password
) {
    @Override
    public String toString() {
        return "CreateAccountCommand[redacted]";
    }
}
