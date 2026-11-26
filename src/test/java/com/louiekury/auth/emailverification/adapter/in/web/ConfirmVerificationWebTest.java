package com.louiekury.auth.emailverification.adapter.in.web;

import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationWebAdapter;
import com.louiekury.auth.emailverification.application.createemailverification.CreateEmailVerificationUseCase;
import com.louiekury.auth.emailverification.application.createemailverification.SendVerificationResult;
import com.louiekury.auth.emailverification.application.updateemailverification.ConfirmVerificationResult;
import com.louiekury.auth.emailverification.application.updateemailverification.UpdateEmailVerificationUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfirmVerificationWebTest {
    private final CreateEmailVerificationUseCase send = mock(CreateEmailVerificationUseCase.class);
    private final UpdateEmailVerificationUseCase confirm = mock(UpdateEmailVerificationUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new EmailVerificationController(new EmailVerificationWebAdapter(send,
            confirm)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("이메일 발송 요청은 실제 원격 주소를 전달하고 202를 반환한다")
    void sendReturnsAcceptedAndPassesRemoteAddress() throws Exception {
        when(send.createEmailVerification("user@example.com", "SIGNUP", "192.0.2.15"))
            .thenReturn(SendVerificationResult.of("verification_1", Instant.parse("2026-11-24T08:00:00Z")));
        mvc.perform(post("/api/auth/email-verifications")
                .with(request -> { request.setRemoteAddr("192.0.2.15"); return request; })
                .header("X-Forwarded-For", "203.0.113.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"purpose\":\"SIGNUP\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.verificationId").value("verification_1"));
    }
    @Test
    @DisplayName("인증 성공 시 증명과 만료 시각을 반환한다")
    void confirmationReturnsExpectedJsonAndExpiry() throws Exception {
        when(confirm.updateEmailVerificationConfirmed("verify_123", "001234")).thenReturn(
            ConfirmVerificationResult.of("proof-token", Instant.parse("2026-11-24T08:00:00Z")));
        mvc.perform(post("/api/auth/email-verifications/verify_123/confirm")
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"001234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.emailVerificationToken").value("proof-token"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T17:00:00+09:00"));
    }

    @Test
    @DisplayName("인증번호가 틀리면 오류를 반환하고 증명을 제공하지 않는다")
    void incorrectCodeIsReportedWithoutProof() throws Exception {
        when(confirm.updateEmailVerificationConfirmed("verify_123", "999999")).thenThrow(new
            BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        mvc.perform(post("/api/auth/email-verifications/verify_123/confirm")
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"999999\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
