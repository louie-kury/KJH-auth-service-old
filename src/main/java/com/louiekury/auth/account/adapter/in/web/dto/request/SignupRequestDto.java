package com.louiekury.auth.account.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.louiekury.auth.account.adapter.in.web.ValidSignupBirthDate;
import com.louiekury.auth.account.adapter.in.web.ValidSignupEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.CodePointLength;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(name = "SignupRequest")
public class SignupRequestDto {
    @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name은(는) 필수입니다.")
    @CodePointLength(max = 100, message = "name은(는) 최대 100자입니다.")
    private final String name;
    @Schema(description = "필수, 숫자만, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "phoneNumber은(는) 필수입니다.")
    @CodePointLength(max = 30, message = "phoneNumber은(는) 최대 30자입니다.")
    @Pattern(regexp = "[0-9]+", message = "phoneNumber는 숫자만 입력해야 합니다.")
    private final String phoneNumber;
    @Schema(allowableValues = {"MALE", "FEMALE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "gender은(는) 필수입니다.")
    @CodePointLength(max = 20, message = "gender은(는) 최대 20자입니다.")
    @Pattern(regexp = "MALE|FEMALE", message = "gender는 MALE 또는 FEMALE이어야 합니다.")
    private final String gender;
    @Schema(description = "필수, 1000-01-01부터 9999-12-31까지", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "birthDate는 필수입니다.")
    @ValidSignupBirthDate
    private final LocalDate birthDate;
    @Schema(description = "필수, 최대 255 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "address은(는) 필수입니다.")
    @CodePointLength(max = 255, message = "address은(는) 최대 255자입니다.")
    private final String address;
    @Schema(description = "필수, 최대 30 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "nickname은(는) 필수입니다.")
    @CodePointLength(max = 30, message = "nickname은(는) 최대 30자입니다.")
    private final String nickname;
    @Schema(description = "필수, 최대 100 코드 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "loginId은(는) 필수입니다.")
    @CodePointLength(max = 100, message = "loginId은(는) 최대 100자입니다.")
    private final String loginId;
    @Schema(description = "필수, 8~128 코드 포인트", example = "<password>", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "password는 필수입니다.")
    @CodePointLength(min = 8, max = 128, message = "password는 8~128자여야 합니다.")
    private final String password;
    @Schema(description = "필수 ASCII 이메일, 최대 254자", example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "email은(는) 필수입니다.")
    @CodePointLength(max = 254, message = "email은(는) 최대 254자입니다.")
    @ValidSignupEmail
    private final String email;
    @Schema(description = "필수 인증 증명, 최대 100 코드 포인트", example = "<email-verification-token>",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "emailVerificationToken은(는) 필수입니다.")
    @CodePointLength(max = 100, message = "emailVerificationToken은(는) 최대 100자입니다.")
    private final String emailVerificationToken;

    private SignupRequestDto(
            String name,
            String phoneNumber,
            String gender,
            LocalDate birthDate,
            String address,
            String nickname,
            String loginId,
            String password,
            String email,
            String emailVerificationToken) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.nickname = nickname;
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.emailVerificationToken = emailVerificationToken;
    }

    @JsonCreator
    public static SignupRequestDto of(
            @JsonProperty("name") String name,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("gender") String gender,
            @JsonProperty("birthDate") LocalDate birthDate,
            @JsonProperty("address") String address,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("loginId") String loginId,
            @JsonProperty("password") String password,
            @JsonProperty("email") String email,
            @JsonProperty("emailVerificationToken") String emailVerificationToken) {
        return SignupRequestDto.builder().name(name).phoneNumber(phoneNumber).gender(gender).birthDate(birthDate)
            .address(address).nickname(nickname).loginId(loginId).password(password).email(email)
            .emailVerificationToken(emailVerificationToken).build();
    }

    @Override
    public String toString() {
        return "SignupRequest[redacted]";
    }
}
