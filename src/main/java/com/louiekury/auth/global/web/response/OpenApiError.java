package com.louiekury.auth.global.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 오류 응답 형식. */
public record OpenApiError(
        @Schema(description = "HTTP 표준 상태 문구", example = "Conflict") String title,
        @Schema(description = "HTTP 상태 코드", example = "409") int status,
        @Schema(description = "안전한 오류 설명", example = "이미 가입된 이메일입니다.") String detail,
        @Schema(description = "서비스 오류 코드", example = "EMAIL_ALREADY_EXISTS") String code) { }
