package com.louiekury.auth.account.application.createsignup;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CreateSignupCommand {
    private final String name;
    private final String phoneNumber;
    private final String gender;
    private final LocalDate birthDate;
    private final String address;
    private final String nickname;
    private final String loginId;
    private final String password;
    private final String email;
    private final String emailVerificationToken;

    public static CreateSignupCommand of(String name, String phoneNumber, String gender, LocalDate birthDate,
            String address, String nickname, String loginId, String password, String email,
            String emailVerificationToken) {
        return CreateSignupCommand.builder()
            .name(name).phoneNumber(phoneNumber).gender(gender).birthDate(birthDate).address(address)
            .nickname(nickname).loginId(loginId).password(password).email(email)
            .emailVerificationToken(emailVerificationToken).build();
    }

    @Override
    public String toString() {
        return "SignupCommand[redacted]";
    }
}
