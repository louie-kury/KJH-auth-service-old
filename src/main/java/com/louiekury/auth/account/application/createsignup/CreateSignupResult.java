package com.louiekury.auth.account.application.createsignup;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class CreateSignupResult {
    private final String memberId;

    public static CreateSignupResult of(String memberId) {
        return CreateSignupResult.builder().memberId(memberId).build();
    }
}