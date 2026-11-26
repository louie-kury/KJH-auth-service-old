package com.louiekury.auth.account.application.login;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GetLoginAccountResult {
    private final String memberId;
    private final String passwordHash;
    private final boolean available;

    public static GetLoginAccountResult of(String memberId, String passwordHash, boolean available) {
        return GetLoginAccountResult.builder().memberId(memberId).passwordHash(passwordHash).available(available).build();
    }
}
