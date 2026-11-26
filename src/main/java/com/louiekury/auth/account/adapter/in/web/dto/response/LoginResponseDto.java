package com.louiekury.auth.account.adapter.in.web.dto.response;

import com.louiekury.auth.account.application.login.CreateLoginResult;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponseDto {
    private final String accessToken;
    private final String tokenType;
    private final Instant expiresAt;
    private final String memberId;

    public static LoginResponseDto from(CreateLoginResult result) {
        return LoginResponseDto.builder().accessToken(result.getAccessToken()).tokenType(result.getTokenType()).expiresAt(result.getExpiresAt()).memberId(result.getMemberId()).build();
    }

    @Override
    public String toString() { return "LoginResponseDto[redacted]"; }
}
