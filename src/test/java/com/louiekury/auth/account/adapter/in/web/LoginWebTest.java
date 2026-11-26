package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.application.login.CreateLoginUseCase;
import com.louiekury.auth.account.application.login.CreateLoginResult;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginWebTest {
    private final CreateLoginUseCase service = mock(CreateLoginUseCase.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new LoginController(new LoginWebAdapter(service)))
        .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("정상 로그인 요청에 토큰과 회원 식별자를 반환한다")
    void validRequestReturnsTokenAndMember() throws Exception {
        when(service.createLogin(argThat(command -> command.getLoginId().equals("user") && command.getPassword().equals("password123")))).thenReturn(
            CreateLoginResult.of("signed-token", "Bearer", Instant.parse("2030-01-01T00:00:00Z"), "member"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"user\",\"password\":\"password123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").value("signed-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.memberId").value("member"))
            .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    @DisplayName("잘못된 입력은 인증 전에 거부한다")
    void malformedInputIsRejectedBeforeAuthentication() throws Exception {
        for (String body : new String[]{"{}", "{\"loginId\":\" \",\"password\":\"password123\"}",
                "{\"loginId\":\"user\",\"password\":\"short\"}"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("인증 실패와 계정 오류를 HTTP 오류로 변환한다")
    void credentialsAndAccountErrorsAreMapped() throws Exception {
        when(service.createLogin(argThat(command -> command.getLoginId().equals("user") && command.getPassword().equals("password123")))).thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"user\",\"password\":\"password123\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        doThrow(new BusinessException(ErrorCode.ACCOUNT_UNAVAILABLE)).when(service).createLogin(argThat(command -> command.getLoginId().equals("user") && command.getPassword().equals("password123")));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"user\",\"password\":\"password123\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_UNAVAILABLE"));
    }
}
