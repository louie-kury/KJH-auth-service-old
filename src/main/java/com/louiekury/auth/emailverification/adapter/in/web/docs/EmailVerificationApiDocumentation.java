package com.louiekury.auth.emailverification.adapter.in.web.docs;

import com.louiekury.auth.emailverification.adapter.in.web.dto.request.CreateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.request.UpdateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.CreateEmailVerificationResponseDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.UpdateEmailVerificationResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.global.web.response.OpenApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface EmailVerificationApiDocumentation {
    @Operation(summary = "이메일 인증번호 발송",
        description = "회원가입용 인증번호 발송을 요청합니다. 현재 SIGNUP 목적만 지원합니다. "
            + "발송 요청 수락은 실제 이메일 수신을 보장하지 않습니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "발송 요청이 수락되어 인증 요청 ID와 만료 시각을 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "VALIDATION_ERROR: 이메일 형식이나 발송 목적 등 요청 값이 올바르지 않습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "EMAIL_ALREADY_EXISTS: 이미 가입된 이메일입니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description = "RATE_LIMITED: 인증번호 발송 또는 확인 요청이 허용 횟수를 초과했습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "VERIFICATION_DELIVERY_FAILED: 인증 요청을 저장하거나 이메일을 발송할 수 없습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    ResponseEntity<ApiResponse<CreateEmailVerificationResponseDto>> createEmailVerification(
        CreateEmailVerificationRequestDto request,
        HttpServletRequest servletRequest
    );

    @Operation(summary = "이메일 인증번호 확인",
        description = "발송 응답의 인증 요청 ID와 숫자 6자리 인증번호를 확인하고 일회성 가입 증명을 발급합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증에 성공하여 가입 증명과 만료 시각을 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "VALIDATION_ERROR: 인증 요청 ID 또는 번호 형식이 올바르지 않습니다. "
            + "INVALID_VERIFICATION_CODE: 인증번호가 일치하지 않습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "NOT_FOUND: 인증 요청이 없거나 이미 사용 또는 무효화되었습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "VERIFICATION_EXPIRED: 인증 요청의 유효기간이 지났습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",
        description = "RATE_LIMITED: 인증번호 발송 또는 확인 요청이 허용 횟수를 초과했습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "503",
        description = "VERIFICATION_UNAVAILABLE: 인증번호를 확인할 수 없습니다.",
        content = @Content(schema = @Schema(implementation = OpenApiError.class))
    )
    ResponseEntity<ApiResponse<UpdateEmailVerificationResponseDto>> updateEmailVerificationConfirmed(
        String verificationId,
        UpdateEmailVerificationRequestDto request
    );

}
