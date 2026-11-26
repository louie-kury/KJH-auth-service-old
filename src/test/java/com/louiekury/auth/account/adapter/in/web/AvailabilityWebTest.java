package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.adapter.in.web.AvailabilityWebAdapter;
import com.louiekury.auth.account.application.getaccountavailability.GetAccountAvailabilityResult;
import com.louiekury.auth.account.application.getaccountavailability.GetAccountAvailabilityUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityWebTest {
    private final GetAccountAvailabilityUseCase availability = mock(GetAccountAvailabilityUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AvailabilityController(new AvailabilityWebAdapter(availability)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("사용 가능한 아이디는 true를 반환한다")
    void availableLoginIdReturnsTrue() throws Exception {
        when(availability.getLoginIdAvailability("Example")).thenReturn(GetAccountAvailabilityResult.of(true));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "Example"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("사용 중인 아이디는 false를 반환한다")
    void occupiedLoginIdReturnsFalse() throws Exception {
        when(availability.getLoginIdAvailability("existing")).thenReturn(GetAccountAvailabilityResult.of(false));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "existing"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("사용 가능한 닉네임은 true를 반환한다")
    void availableNicknameReturnsTrue() throws Exception {
        when(availability.getNicknameAvailability("루이")).thenReturn(GetAccountAvailabilityResult.of(true));
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "루이"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("사용 중인 닉네임은 false를 반환한다")
    void occupiedNicknameReturnsFalse() throws Exception {
        when(availability.getNicknameAvailability("existing")).thenReturn(GetAccountAvailabilityResult.of(false));
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "existing"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("아이디나 닉네임이 누락되거나 공백이면 거절한다")
    void missingOrBlankValuesAreRejected() throws Exception {
        mvc.perform(get("/api/auth/login-ids/availability")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/nicknames/availability")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "  ")).andExpect(status().isBadRequest());
        verifyNoInteractions(availability);
    }

    @Test
    @DisplayName("아이디 길이는 유니코드 코드 포인트로 계산한다")
    void loginIdLengthUsesUnicodeCodePoints() throws Exception {
        String boundary = "😀".repeat(100);
        when(availability.getLoginIdAvailability(boundary)).thenReturn(GetAccountAvailabilityResult.of(true));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", boundary))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", boundary + "a"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 길이는 유니코드 코드 포인트로 계산한다")
    void nicknameLengthUsesUnicodeCodePoints() throws Exception {
        String boundary = "😀".repeat(30);
        when(availability.getNicknameAvailability(boundary)).thenReturn(GetAccountAvailabilityResult.of(true));
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", boundary))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", boundary + "a"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공백이 아닌 아이디 입력을 변환하지 않는다")
    void nonBlankInputIsNotTransformed() throws Exception {
        when(availability.getLoginIdAvailability(" User ")).thenReturn(GetAccountAvailabilityResult.of(true));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", " User "))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        verify(availability).getLoginIdAvailability(" User ");
    }
}
