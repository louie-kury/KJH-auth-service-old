package com.louiekury.auth.emailverification.adapter.in.web.dto.response;

import com.louiekury.auth.emailverification.application.updateemailverification.ConfirmVerificationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Schema(name = "ConfirmResponse")
public class UpdateEmailVerificationResponseDto {
    private final String emailVerificationToken;
    private final OffsetDateTime expiresAt;

    public static UpdateEmailVerificationResponseDto from(ConfirmVerificationResult result) {
        return UpdateEmailVerificationResponseDto.builder()
            .emailVerificationToken(result.getEmailVerificationToken())
            .expiresAt(result.getExpiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
            .build();
    }
}
