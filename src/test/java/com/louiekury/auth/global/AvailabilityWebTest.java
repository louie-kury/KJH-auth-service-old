package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvailabilityWebTest {
    private final AvailabilityUseCase availability = mock(AvailabilityUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AvailabilityController(availability))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void availableLoginIdReturnsTrue() throws Exception {
        when(availability.isLoginIdAvailable("Example")).thenReturn(true);
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "Example"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void occupiedLoginIdReturnsFalse() throws Exception {
        when(availability.isLoginIdAvailable("existing")).thenReturn(false);
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "existing"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void availableNicknameReturnsTrue() throws Exception {
        when(availability.isNicknameAvailable("루이")).thenReturn(true);
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "루이"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void occupiedNicknameReturnsFalse() throws Exception {
        when(availability.isNicknameAvailable("existing")).thenReturn(false);
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "existing"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void missingOrBlankValuesAreRejected() throws Exception {
        mvc.perform(get("/api/auth/login-ids/availability")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/nicknames/availability")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", "  ")).andExpect(status().isBadRequest());
        verifyNoInteractions(availability);
    }

    @Test
    void loginIdLengthUsesUnicodeCodePoints() throws Exception {
        String boundary = "😀".repeat(100);
        when(availability.isLoginIdAvailable(boundary)).thenReturn(true);
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", boundary))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", boundary + "a"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nicknameLengthUsesUnicodeCodePoints() throws Exception {
        String boundary = "😀".repeat(30);
        when(availability.isNicknameAvailable(boundary)).thenReturn(true);
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", boundary))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        mvc.perform(get("/api/auth/nicknames/availability").param("nickname", boundary + "a"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonBlankInputIsNotTransformed() throws Exception {
        when(availability.isLoginIdAvailable(" User ")).thenReturn(true);
        mvc.perform(get("/api/auth/login-ids/availability").param("loginId", " User "))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(true));
        verify(availability).isLoginIdAvailable(" User ");
    }
}
