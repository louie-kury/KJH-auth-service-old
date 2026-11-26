package com.louiekury.auth.global.web;

import com.louiekury.auth.global.web.exception.ErrorCodeHttpMapper;
import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.global.web.exception.WebErrorCode;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebContractTest {
    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new ContractController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("성공 응답에는 데이터만 포함한다")
    void successHasOnlyData() throws Exception {
        mvc.perform(get("/contract"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\":{\"name\":\"루이\"}}"))
            .andExpect(jsonPath("$.code").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("업무 오류에는 지정한 메시지를 포함하고 데이터는 제외한다")
    void businessErrorPublishesChosenMessageWithoutData() throws Exception {
        mvc.perform(get("/contract/business"))
            .andExpect(status().isForbidden())
            .andExpect(content().json("{\"code\":\"FORBIDDEN\",\"title\":\"Forbidden\",\"status\":403,\"detai" +
                "l\":\"공개 가능한 설명\"}"))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("사용자 오류 메시지가 없으면 기본 메시지를 사용한다")
    void businessErrorUsesDefaultMessageWhenCustomMessageIsNull() throws Exception {
        mvc.perform(get("/contract/business/default-message"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"custom-message", "cause"})
    @DisplayName("내부 업무 오류에서 민감한 상세 정보를 숨긴다")
    void internalBusinessErrorHidesSensitiveDetails(String construction) throws Exception {
        String body = mvc.perform(get("/contract/business-internal/" + construction))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret-password", "internal-host", "IllegalStateException");
    }

    @ParameterizedTest
    @CsvSource({"VALIDATION_ERROR,400", "FORBIDDEN,403", "NOT_FOUND,404", "INTERNAL_SERVER_ERROR,500",
        "EMAIL_ALREADY_EXISTS,409", "RATE_LIMITED,429", "VERIFICATION_DELIVERY_FAILED,503",
        "INVALID_VERIFICATION_CODE,400", "VERIFICATION_EXPIRED,409", "VERIFICATION_UNAVAILABLE,503"})
    @DisplayName("오류 코드에 기대하는 HTTP 상태를 반환한다")
    void errorCodesHaveExpectedHttpStatus(ErrorCode code, int expected) {
        assertThat(ErrorCodeHttpMapper.toStatus(code).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"UNAUTHORIZED,401", "METHOD_NOT_ALLOWED,405"})
    @DisplayName("인증 및 메서드 오류의 이름과 HTTP 상태를 유지한다")
    void webErrorCodesKeepExpectedNamesAndHttpStatuses(String name, int expected) {
        WebErrorCode code = WebErrorCode.valueOf(name);
        assertThat(code.name()).isEqualTo(name);
        assertThat(code.getStatus().value()).isEqualTo(expected);
    }
    @Test
    @DisplayName("모든 오류 코드를 HTTP 상태로 매핑한다")
    void mappingCoversEntireErrorVocabulary() {
        assertThat(ErrorCode.values()).isNotEmpty();
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(ErrorCodeHttpMapper.toStatus(code)).isNotNull();
        }
    }

    @Test
    @DisplayName("잘못된 JSON에 안전한 400 오류를 반환한다")
    void malformedJsonIsSafeBadRequest() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.APPLICATION_JSON).content("{secret-invalid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Bad Request")).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("유효하지 않은 입력에는 400 오류를 반환한다")
    void invalidInputIsBadRequest() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Bad Request")).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("없는 경로에는 404 오류를 반환한다")
    void missingRouteIsNotFound() throws Exception {
        mvc.perform(get("/missing"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드에서 Allow 헤더를 유지한다")
    void unsupportedMethodKeepsAllowHeader() throws Exception {
        String allow = mvc.perform(delete("/contract"))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Method Not Allowed"))
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.detail").isNotEmpty())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andReturn().getResponse().getHeader("Allow");
        assertThat(allow).contains("GET", "POST");
    }

    @Test
    @DisplayName("지원하지 않는 미디어 타입에 415 오류를 반환한다")
    void unsupportedMediaTypeKeeps415() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.TEXT_PLAIN).content("루이"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("예상하지 못한 오류에서 내부 상세 정보를 숨긴다")
    void unexpectedErrorHidesInternalDetails() throws Exception {
        mvc.perform(get("/contract/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().json("{\"code\":\"INTERNAL_SERVER_ERROR\",\"title\":\"Internal Server Error" +
                "\",\"status\":500,\"detail\":\"서버 내부 오류가 발생했습니다.\"}"))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class ContractController {
        @GetMapping("/contract")
        ApiResponse<Map<String,String>> success() { return ApiResponse.success(Map.of("name", "루이")); }
        @PostMapping(value = "/contract", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Name> create(@Valid @RequestBody Name name) { return ApiResponse.success(name); }
        @GetMapping("/contract/business")
        void business() { throw new BusinessException(ErrorCode.FORBIDDEN, "공개 가능한 설명"); }
        @GetMapping("/contract/business/default-message")
        void businessWithoutCustomMessage() {
            throw new BusinessException(ErrorCode.FORBIDDEN, (String) null);
        }
        @GetMapping("/contract/business-internal/custom-message")
        void internalBusinessMessage() {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "jdbc:secret-password internal-host");
        }
        @GetMapping("/contract/business-internal/cause")
        void internalBusinessCause() {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                new IllegalStateException("jdbc:secret-password internal-host"));
        }
        @GetMapping("/contract/unexpected")
        void unexpected() { throw new IllegalStateException("jdbc:secret-password internal-host"); }
    }
    static class Name {
        @NotBlank
        public String name;
    }
}
