package com.louiekury.auth.global;

import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConfirmVerificationWebTest {
    private final ConfirmVerificationUseCase confirm = mock(ConfirmVerificationUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new EmailVerificationController(mock(SendVerificationUseCase.class), confirm))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void confirmationReturnsExpectedJsonAndExpiry() throws Exception {
        when(confirm.confirm("verify_123", "001234")).thenReturn(
            new ConfirmVerificationUseCase.Result("proof-token", Instant.parse("2026-11-24T08:00:00Z")));
        mvc.perform(post("/api/auth/email-verifications/verify_123/confirm")
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"001234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.emailVerificationToken").value("proof-token"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T17:00:00+09:00"));
    }

    @Test
    void incorrectCodeIsReportedWithoutProof() throws Exception {
        when(confirm.confirm("verify_123", "999999")).thenThrow(new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        mvc.perform(post("/api/auth/email-verifications/verify_123/confirm")
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"999999\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
