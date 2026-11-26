package com.louiekury.auth.emailverification.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Schema(name = "ConfirmRequest")
public class UpdateEmailVerificationRequestDto {
    @NotNull
    @Pattern(regexp = "[0-9]{6}")
    @Schema(description = "필수 6자리 인증번호", example = "<6-digit-code>", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String code;

@JsonCreator
    public static UpdateEmailVerificationRequestDto of(@JsonProperty("code") String code) {
        return UpdateEmailVerificationRequestDto.builder().code(code).build();
    }
}
