package com.louiekury.auth.account.adapter.in.web.dto.response;

import com.louiekury.auth.account.application.createsignup.CreateSignupResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(name = "SignupResponse")
public class SignupResponseDto {
    private final String memberId;

    private SignupResponseDto(String memberId) {
        this.memberId = memberId;
    }
    public static SignupResponseDto from(CreateSignupResult result) {
        return SignupResponseDto.builder().memberId(result.getMemberId()).build();
    }
}
