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
@Schema(name = "EmailVerificationSendRequest")
public class CreateEmailVerificationRequestDto {
    @NotNull
    @Schema(description = "필수 ASCII 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String email;

    @NotNull
    @Pattern(regexp = "SIGNUP")
    @Schema(allowableValues = {"SIGNUP"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private final String purpose;

@JsonCreator
    public static CreateEmailVerificationRequestDto of(@JsonProperty("email") String email,
            @JsonProperty("purpose") String purpose) {
        return CreateEmailVerificationRequestDto.builder().email(email).purpose(purpose).build();
    }
}
