package com.louiekury.auth.emailverification.adapter.in.web;

import com.louiekury.auth.emailverification.adapter.in.web.docs.EmailVerificationApiDocumentation;

import com.louiekury.auth.emailverification.adapter.in.web.dto.request.CreateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.request.UpdateEmailVerificationRequestDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.CreateEmailVerificationResponseDto;
import com.louiekury.auth.emailverification.adapter.in.web.dto.response.UpdateEmailVerificationResponseDto;
import com.louiekury.auth.global.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController implements EmailVerificationApiDocumentation {
    private final EmailVerificationWebAdapter adapter;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<CreateEmailVerificationResponseDto>> createEmailVerification(
        @Valid @RequestBody CreateEmailVerificationRequestDto request,
        HttpServletRequest servletRequest
    ) {
        return adapter.createEmailVerification(request, servletRequest);
    }

    @Override
    @PostMapping("/{verificationId}/confirm")
    public ResponseEntity<ApiResponse<UpdateEmailVerificationResponseDto>> updateEmailVerificationConfirmed(
        @PathVariable String verificationId,
        @Valid @RequestBody UpdateEmailVerificationRequestDto request
    ) {
        return adapter.updateEmailVerificationConfirmed(verificationId, request);
    }
}
