package com.louiekury.auth.account.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.CodePointLength;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateLoginRequestDto {
    @NotBlank
    @CodePointLength(max = 100)
    private final String loginId;

    @NotBlank
    @CodePointLength(min = 8, max = 128)
    private final String password;

    @JsonCreator
    public static CreateLoginRequestDto of(@JsonProperty("loginId") String loginId,
                                     @JsonProperty("password") String password) {
        return CreateLoginRequestDto.builder().loginId(loginId).password(password).build();
    }

    @Override
    public String toString() { return "CreateLoginRequestDto[redacted]"; }
}
