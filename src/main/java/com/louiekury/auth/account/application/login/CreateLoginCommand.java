package com.louiekury.auth.account.application.login;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateLoginCommand {
    private final String loginId;
    private final String password;

    public static CreateLoginCommand of(String loginId, String password) {
        return CreateLoginCommand.builder().loginId(loginId).password(password).build();
    }
}
