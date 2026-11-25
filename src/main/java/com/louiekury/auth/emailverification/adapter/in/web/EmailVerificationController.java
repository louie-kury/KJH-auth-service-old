package com.louiekury.auth.emailverification.adapter.in.web;

import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {
    private final SendVerificationUseCase useCase;
    private final ConfirmVerificationUseCase confirmUseCase;

    public EmailVerificationController(SendVerificationUseCase useCase, ConfirmVerificationUseCase confirmUseCase) {
        this.useCase = useCase;
        this.confirmUseCase = confirmUseCase;
    }

    @Operation(summary = "이메일 인증번호 발송")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "발송 요청 접수")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "RATE_LIMITED", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "VERIFICATION_UNAVAILABLE 또는 VERIFICATION_DELIVERY_FAILED", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Response> send(@RequestBody Request request, HttpServletRequest servletRequest) {
        var result = useCase.send(request.email(), request.purpose(), servletRequest.getRemoteAddr());
        return ApiResponse.success(new Response(result.verificationId(), result.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()));
    }

    @Operation(summary = "이메일 인증번호 확인")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR 또는 INVALID_VERIFICATION_CODE", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "NOT_FOUND", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "VERIFICATION_EXPIRED", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "RATE_LIMITED", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "VERIFICATION_UNAVAILABLE", content = @io.swagger.v3.oas.annotations.media.Content(schema = @Schema(implementation = com.louiekury.auth.global.web.response.OpenApiError.class)))
    @PostMapping("/{verificationId}/confirm")
    public ApiResponse<ConfirmResponse> confirm(@PathVariable String verificationId, @RequestBody ConfirmRequest request) {
        var result = confirmUseCase.confirm(verificationId, request.code());
        return ApiResponse.success(new ConfirmResponse(result.emailVerificationToken(),
                result.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()));
    }

    public record Request(@Schema(description = "필수 ASCII 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email, @Schema(allowableValues = {"SIGNUP"}, requiredMode = Schema.RequiredMode.REQUIRED) String purpose) {}
    public record Response(String verificationId, OffsetDateTime expiresAt) {}
    public record ConfirmRequest(@Schema(description = "필수 6자리 인증번호", example = "<6-digit-code>", requiredMode = Schema.RequiredMode.REQUIRED) String code) {}
    public record ConfirmResponse(String emailVerificationToken, OffsetDateTime expiresAt) {}
}
