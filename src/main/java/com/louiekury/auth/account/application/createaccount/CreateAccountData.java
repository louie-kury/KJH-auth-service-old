package com.louiekury.auth.account.application.createaccount;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CreateAccountData {
    private final String uuid;
    private final String status;
    private final String name;
    private final String phoneNumber;
    private final String gender;
    private final LocalDate birthDate;
    private final String address;
    private final String email;
    private final boolean termsAgreed;
    private final String loginId;
    private final String passwordHash;

    public static CreateAccountData of(String uuid, String status, String name, String phoneNumber,
            String gender, LocalDate birthDate, String address, String email, boolean termsAgreed,
            String loginId, String passwordHash) {
        return CreateAccountData.builder()
            .uuid(uuid).status(status).name(name).phoneNumber(phoneNumber).gender(gender).birthDate(birthDate)
            .address(address).email(email).termsAgreed(termsAgreed).loginId(loginId).passwordHash(passwordHash).build();
    }

    @Override
    public String toString() {
        return "AccountData[redacted]";
    }
}
