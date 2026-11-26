package com.louiekury.auth.emailverification.adapter.in.web.dto.response;

import com.louiekury.auth.emailverification.application.createemailverification.SendVerificationResult;
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
@Schema(name = "EmailVerificationSendResponse")
public class CreateEmailVerificationResponseDto {
    private final String verificationId;
    private final OffsetDateTime expiresAt;

    public static CreateEmailVerificationResponseDto from(SendVerificationResult result) {
        return CreateEmailVerificationResponseDto.builder()
            .verificationId(result.getVerificationId())
            .expiresAt(result.getExpiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
            .build();
    }
}
