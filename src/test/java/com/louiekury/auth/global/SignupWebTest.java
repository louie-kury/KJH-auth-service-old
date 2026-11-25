package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignupWebTest {
    private final SignupUseCase signup = mock(SignupUseCase.class);
    private MockMvc mvc;
    private static final String REQUEST = """
        {"name":"홍길동","phoneNumber":"01012345678","gender":"MALE",
         "birthDate":"1995-01-01","address":"서울","nickname":"nickname",
         "loginId":"test-login","password":"long-password-12345",
         "email":"test@example.com","emailVerificationToken":"test-proof"}
        """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(signup))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void signupReturnsCreatedMemberAndLocation() throws Exception {
        when(signup.signup(eq("request-key"), any())).thenReturn("member_01");
        mvc.perform(post("/api/auth/signup").header("Idempotency-Key", "request-key")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/members/me"))
            .andExpect(jsonPath("$.data.memberId").value("member_01"));
    }

    @Test
    void allSpacePasswordOfValidLengthIsAccepted() throws Exception {
        when(signup.signup(eq("space-key"), any())).thenReturn("member_01");
        String request = REQUEST.replace("long-password-12345", "               ");
        mvc.perform(post("/api/auth/signup").header("Idempotency-Key", "space-key")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated());
    }
    @Test
    void missingIdempotencyKeyIsRejected() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }

    @Test
    void missingRequiredInputIsRejected() throws Exception {
        mvc.perform(post("/api/auth/signup").header("Idempotency-Key", "request-key")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void changedRequestWithSameKeyReturnsConflict() throws Exception {
        when(signup.signup(eq("request-key"), any()))
            .thenThrow(new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT));
        mvc.perform(post("/api/auth/signup").header("Idempotency-Key", "request-key")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void invalidEmailProofDoesNotCreateAccount() throws Exception {
        when(signup.signup(eq("request-key"), any()))
            .thenThrow(new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION));
        mvc.perform(post("/api/auth/signup").header("Idempotency-Key", "request-key")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION"));
    }
}
