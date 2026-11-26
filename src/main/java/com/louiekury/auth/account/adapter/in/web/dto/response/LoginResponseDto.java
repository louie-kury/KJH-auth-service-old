package com.louiekury.auth.account.adapter.in.web.dto.response;

import com.louiekury.auth.account.application.login.CreateLoginResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(name = "LoginResponse")
public class LoginResponseDto {
    @Schema(description = "RS256 서명 액세스 토큰", example = "<access-token>")
    private final String accessToken;
    @Schema(description = "토큰 인증 방식", example = "Bearer")
    private final String tokenType;
    @Schema(description = "액세스 토큰 만료 시각")
    private final Instant expiresAt;
    @Schema(description = "로그인한 회원의 식별자")
    private final String memberId;

    public static LoginResponseDto from(CreateLoginResult result) {
        return LoginResponseDto.builder().accessToken(result.getAccessToken()).tokenType(result.getTokenType()).expiresAt(result.getExpiresAt()).memberId(result.getMemberId()).build();
    }

    @Override
    public String toString() { return "LoginResponseDto[redacted]"; }
}
