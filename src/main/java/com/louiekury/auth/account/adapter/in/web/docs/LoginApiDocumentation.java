package com.louiekury.auth.account.adapter.in.web.docs;

import com.louiekury.auth.account.adapter.in.web.dto.request.CreateLoginRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.LoginResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.global.web.response.OpenApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;

public interface LoginApiDocumentation {
    @Operation(summary = "일반 로그인",
        description = "아이디와 비밀번호로 로그인하여 15분간 유효한 액세스 토큰을 발급합니다. "
            + "리프레시 쿠키와 토큰 갱신은 제공하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200", description = "로그인에 성공하여 액세스 토큰과 회원 식별자를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400", description = "VALIDATION_ERROR: 요청 값이 올바르지 않습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401", description = "INVALID_CREDENTIALS: 아이디 또는 비밀번호가 올바르지 않습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403", description = "ACCOUNT_UNAVAILABLE: 사용할 수 없는 계정입니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    ResponseEntity<ApiResponse<LoginResponseDto>> createLogin(CreateLoginRequestDto request);
}