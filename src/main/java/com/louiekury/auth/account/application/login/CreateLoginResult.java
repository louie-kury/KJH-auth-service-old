package com.louiekury.auth.account.application.login;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateLoginResult {
    private final String accessToken;
    private final String tokenType;
    private final Instant expiresAt;
    private final String memberId;

    public static CreateLoginResult of(String accessToken, String tokenType, Instant expiresAt, String memberId) {
        return CreateLoginResult.builder().accessToken(accessToken).tokenType(tokenType).expiresAt(expiresAt).memberId(memberId).build();
    }

    @Override
    public String toString() { return "CreateLoginResult[redacted]"; }
}
