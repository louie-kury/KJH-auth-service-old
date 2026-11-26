package com.louiekury.auth.account.adapter.in.web.docs;

import com.louiekury.auth.account.adapter.in.web.dto.request.SignupRequestDto;
import com.louiekury.auth.account.adapter.in.web.dto.response.SignupResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.global.web.response.OpenApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;

public interface SignupApiDocumentation {
    @Operation(summary = "일반 회원가입",
        description = "회원정보와 이메일 인증 증명으로 가입합니다. 증명은 한 번만 사용할 수 있으며, "
            + "증명 소비 후 중복 또는 저장 실패가 발생하면 다시 인증해야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201", description = "가입이 완료되어 memberId를 반환합니다. Location 헤더는 /api/members/me입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400", description = "VALIDATION_ERROR: 요청 값이 올바르지 않습니다. "
            + "INVALID_EMAIL_VERIFICATION: 인증 증명이 유효하지 않거나 이미 사용되었습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "EMAIL_ALREADY_EXISTS: 이메일 중복. LOGIN_ID_ALREADY_EXISTS: 아이디 중복. "
            + "NICKNAME_ALREADY_EXISTS: 닉네임 중복.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503", description = "VERIFICATION_UNAVAILABLE: 인증 증명을 확인할 수 없습니다. "
            + "SIGNUP_PENDING: 가입 정보를 저장하거나 중복 원인을 확인하지 못했습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class)))
    ResponseEntity<ApiResponse<SignupResponseDto>> createSignup(SignupRequestDto request);

}
