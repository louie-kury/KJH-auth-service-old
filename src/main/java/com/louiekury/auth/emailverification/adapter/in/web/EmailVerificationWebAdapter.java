package com.louiekury.auth.emailverification.adapter.in.web;

import com.louiekury.auth.emailverification.adapter.in.web.dto.request.CreateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.request.UpdateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.CreateEmailVerificationResponseDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.UpdateEmailVerificationResponseDto;
import com.louiekury.auth.emailverification.application.createemailverification.CreateEmailVerificationUseCase;
import com.louiekury.auth.emailverification.application.updateemailverification.UpdateEmailVerificationUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EmailVerificationWebAdapter {
    private final CreateEmailVerificationUseCase sendUseCase;
    private final UpdateEmailVerificationUseCase confirmUseCase;

    public ResponseEntity<ApiResponse<CreateEmailVerificationResponseDto>> createEmailVerification(
        CreateEmailVerificationRequestDto request,
        HttpServletRequest servletRequest
    ) {
        var result = sendUseCase.createEmailVerification(
            request.getEmail(),
            request.getPurpose(),
            servletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(CreateEmailVerificationResponseDto.from(result)));
    }

    public ResponseEntity<ApiResponse<UpdateEmailVerificationResponseDto>> updateEmailVerificationConfirmed(
        String verificationId,
        UpdateEmailVerificationRequestDto request
    ) {
        var result = confirmUseCase.updateEmailVerificationConfirmed(verificationId, request.getCode());
        return ResponseEntity.ok(ApiResponse.success(UpdateEmailVerificationResponseDto.from(result)));
    }
}
