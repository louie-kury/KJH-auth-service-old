package com.louiekury.auth.account.adapter.in.web;

import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.adapter.in.web.SignupWebAdapter;
import com.louiekury.auth.account.application.createsignup.CreateSignupResult;
import com.louiekury.auth.account.application.createsignup.CreateSignupUseCase;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SignupWebTest {
    private final CreateSignupUseCase signup = mock(CreateSignupUseCase.class);
    private MockMvc mvc;
    private static final String REQUEST = """
        {"name":"홍길동","phoneNumber":"01012345678","gender":"MALE",
         "birthDate":"1995-01-01","address":"서울","nickname":"nickname",
         "loginId":"test-login","password":"long-password-12345",
         "email":"test@example.com","emailVerificationToken":"test-proof"}
        """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SignupController(new SignupWebAdapter(signup)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("회원가입 성공 시 회원 식별자와 위치 헤더를 반환한다")
    void signupReturnsCreatedMemberAndLocation() throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/members/me"))
            .andExpect(jsonPath("$.data.memberId").value("member_01"));
    }

    @Test
    @DisplayName("길이가 유효한 공백 비밀번호를 허용한다")
    void allSpacePasswordOfValidLengthIsAccepted() throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        String request = REQUEST.replace("long-password-12345", "               ");
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {8, 128})
    @DisplayName("비밀번호 길이 경곗값을 허용한다")
    void passwordLengthBoundariesAreAccepted(int length) throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
            .content(REQUEST.replace("long-password-12345", "한".repeat(length))))
            .andExpect(status().isCreated());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {7, 129})
    @DisplayName("비밀번호 길이가 범위를 벗어나면 상세 오류를 반환한다")
    void passwordOutsideLengthBoundariesHasSpecificDetail(int length) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
            .content(REQUEST.replace("long-password-12345", "한".repeat(length))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("password")))
            .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("8")))
            .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("128")))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {8, 128})
    @DisplayName("보충 문자 비밀번호 길이를 코드 포인트로 계산한다")
    void supplementaryUnicodePasswordUsesCodePoints(int length) throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
            .content(REQUEST.replace("long-password-12345", "😀".repeat(length))))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("중복 이메일 가입 시 충돌 오류를 반환한다")
    void duplicateEmailHasConflictProblem() throws Exception {
        ErrorCode duplicateEmail = ErrorCode.valueOf("EMAIL_ALREADY_EXISTS");
        when(signup.createSignup(any())).thenThrow(new BusinessException(duplicateEmail));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").isNotEmpty())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("회원가입에 멱등성 키를 요구하지 않는다")
    void signupDoesNotRequireIdempotencyKey() throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("필수 입력이 누락되면 거절한다")
    void missingRequiredInputIsRejected() throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"0999-12-31", "+10000-01-01"})
    @DisplayName("생년월일이 DB 범위를 벗어나면 증명 사용 전에 거절한다")
    void birthDateOutsideMysqlDateRangeIsRejectedBeforeProofIsConsumed(String birthDate) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("1995-01-01", birthDate)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1000-01-01", "9999-12-31"})
    @DisplayName("DB 날짜 범위의 생년월일 경곗값을 허용한다")
    void birthDateAtMysqlDateBoundariesIsAccepted(String birthDate) throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("1995-01-01", birthDate)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이메일 인증 증명이 유효하지 않으면 계정을 생성하지 않는다")
    void invalidEmailProofDoesNotCreateAccount() throws Exception {
        when(signup.createSignup(any()))
            .thenThrow(new BusinessException(ErrorCode.INVALID_EMAIL_VERIFICATION));
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION"));
    }
    @Test
    @DisplayName("닉네임 길이 제한을 유니코드 코드 포인트로 계산한다")
    void nicknameLimitCountsUnicodeCodePoints() throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        String boundary = "😀".repeat(30);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("\"nickname\":\"nickname\"", "\"nickname\":\"" + boundary + "\"")))
            .andExpect(status().isCreated());
        reset(signup);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("\"nickname\":\"nickname\"", "\"nickname\":\"" + boundary + "a\"")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"010-12345678", "０１０１２３４５６７８"})
    @DisplayName("ASCII 숫자가 아닌 전화번호는 가입 전에 거절한다")
    void nonAsciiOrNondigitPhoneIsRejectedBeforeSignup(String phone) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("01012345678", phone)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"test..user@example.com", "test@-example.com",
        "사용자@example.com"})
    @DisplayName("잘못된 이메일은 가입 전에 거절한다")
    void invalidEmailIsRejectedBeforeSignup(String email) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("test@example.com", email)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(signup);
    }
    @ParameterizedTest
    @MethodSource("invalidRequestsInValidationOrder")
    @DisplayName("여러 필드나 같은 필드가 잘못되면 기존 첫 오류와 상세 메시지를 반환한다")
    void invalidRequestsKeepFirstErrorAndDetail(String request, String detail) throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value(detail));
        verifyNoInteractions(signup);
    }

    private static Stream<Arguments> invalidRequestsInValidationOrder() {
        return Stream.of(
            Arguments.of(REQUEST.replace("홍길동", " ").replace("01012345678", "bad"), "name은(는) 필수입니다."),
            Arguments.of(REQUEST.replace("01012345678", " ".repeat(31)), "phoneNumber은(는) 필수입니다."),
            Arguments.of(REQUEST.replace("01012345678", "x".repeat(31)), "phoneNumber은(는) 최대 30자입니다."),
            Arguments.of(REQUEST.replace("01012345678", "abc").replace("MALE", "OTHER"),
                "phoneNumber는 숫자만 입력해야 합니다."),
            Arguments.of(REQUEST.replace("MALE", " ").replace("1995-01-01", "0999-12-31"),
                "gender은(는) 필수입니다."),
            Arguments.of(REQUEST.replace("test-login", " ").replace("long-password-12345", "short"),
                "loginId은(는) 필수입니다."),
            Arguments.of(REQUEST.replace("long-password-12345", "short").replace("test@example.com", "bad"),
                "password는 8~128자여야 합니다."),
            Arguments.of(REQUEST.replace("test@example.com", "x".repeat(255)), "email은(는) 최대 254자입니다."),
            Arguments.of(REQUEST.replace("test@example.com", "x".repeat(65) + "@example.com"),
                "email은 올바른 ASCII 이메일 형식이어야 합니다.")
        );
    }

    @Test
    @DisplayName("이메일 양쪽 공백은 형식 검사에서 제거하고 원래 입력을 서비스에 전달한다")
    void emailWhitespaceIsTrimmedForValidationButPreservedForService() throws Exception {
        when(signup.createSignup(any())).thenReturn(CreateSignupResult.of("member_01"));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("test@example.com", " test@example.com ")))
            .andExpect(status().isCreated());
        verify(signup).createSignup(argThat(command -> " test@example.com ".equals(command.getEmail())));
    }
}
