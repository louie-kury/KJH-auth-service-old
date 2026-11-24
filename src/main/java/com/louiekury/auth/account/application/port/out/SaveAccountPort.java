package com.louiekury.auth.account.application.port.out;

import java.time.LocalDate;

public interface SaveAccountPort {

    String save(AccountData account);

    record AccountData(
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
            String passwordHash
    ) {
        @Override
        public String toString() {
            return "AccountData[redacted]";
        }
    }
}
