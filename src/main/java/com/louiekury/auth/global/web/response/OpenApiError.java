package com.louiekury.auth.global.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OpenApiError {

    @Schema(description = "HTTP 표준 상태 문구", example = "Conflict")
    private final String title;

    @Schema(description = "HTTP 상태 코드", example = "409")
    private final int status;

    @Schema(description = "안전한 오류 설명", example = "이미 가입된 이메일입니다.")
    private final String detail;

    @Schema(description = "서비스 오류 코드", example = "EMAIL_ALREADY_EXISTS")
    private final String code;

    public static OpenApiError of(String title, int status, String detail, String code) {
        return new OpenApiError(title, status, detail, code);
    }
}
