package com.louiekury.auth.account.application.signup;

import java.time.LocalDate;

public record SignupCommand(String name, String phoneNumber, String gender, LocalDate birthDate,
                            String address, String nickname, String loginId, String password,
                            String email, String emailVerificationToken) {
    @Override
    public String toString() {
        return "SignupCommand[redacted]";
    }
}
