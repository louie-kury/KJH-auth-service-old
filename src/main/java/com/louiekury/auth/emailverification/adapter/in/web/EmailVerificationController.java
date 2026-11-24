package com.louiekury.auth.emailverification.adapter.in.web;

import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
import com.louiekury.auth.global.web.response.ApiResponse;
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

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Response> send(@RequestBody Request request, HttpServletRequest servletRequest) {
        var result = useCase.send(request.email(), request.purpose(), servletRequest.getRemoteAddr());
        return ApiResponse.success(new Response(result.verificationId(), result.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()));
    }

    @PostMapping("/{verificationId}/confirm")
    public ApiResponse<ConfirmResponse> confirm(@PathVariable String verificationId, @RequestBody ConfirmRequest request) {
        var result = confirmUseCase.confirm(verificationId, request.code());
        return ApiResponse.success(new ConfirmResponse(result.emailVerificationToken(),
                result.expiresAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()));
    }

    public record Request(String email, String purpose) {}
    public record Response(String verificationId, OffsetDateTime expiresAt) {}
    public record ConfirmRequest(String code) {}
    public record ConfirmResponse(String emailVerificationToken, OffsetDateTime expiresAt) {}
}
