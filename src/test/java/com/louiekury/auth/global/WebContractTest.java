package com.louiekury.auth.global;

import com.louiekury.auth.global.web.exception.GlobalExceptionHandler;
import com.louiekury.auth.global.web.exception.ErrorCodeHttpMapper;
import com.louiekury.auth.global.web.response.ApiResponse;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebContractTest {
    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new ContractController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void successHasOnlyData() throws Exception {
        mvc.perform(get("/contract"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"data\":{\"name\":\"루이\"}}"))
            .andExpect(jsonPath("$.code").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void businessErrorPublishesChosenMessageWithoutData() throws Exception {
        mvc.perform(get("/contract/business"))
            .andExpect(status().isForbidden())
            .andExpect(content().json("{\"code\":\"FORBIDDEN\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"공개 가능한 설명\"}"))
            .andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
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
    @CsvSource({"VALIDATION_ERROR,400", "UNAUTHORIZED,401", "FORBIDDEN,403", "NOT_FOUND,404", "METHOD_NOT_ALLOWED,405", "INTERNAL_SERVER_ERROR,500", "EMAIL_ALREADY_EXISTS,409", "RATE_LIMITED,429", "VERIFICATION_DELIVERY_FAILED,503", "INVALID_VERIFICATION_CODE,400", "VERIFICATION_EXPIRED,409", "VERIFICATION_UNAVAILABLE,503"})
    void errorCodesHaveExpectedHttpStatus(ErrorCode code, int expected) {
        assertThat(ErrorCodeHttpMapper.toStatus(code).value()).isEqualTo(expected);
    }

    @Test
    void mappingCoversEntireErrorVocabulary() {
        assertThat(ErrorCode.values()).isNotEmpty();
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(ErrorCodeHttpMapper.toStatus(code)).isNotNull();
        }
    }

    @Test
    void malformedJsonIsSafeBadRequest() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.APPLICATION_JSON).content("{secret-invalid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Bad Request")).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    void invalidInputIsBadRequest() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").doesNotExist())
            .andExpect(jsonPath("$.title").value("Bad Request")).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").isNotEmpty()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingRouteIsNotFound() throws Exception {
        mvc.perform(get("/missing"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
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
    void unsupportedMediaTypeKeeps415() throws Exception {
        mvc.perform(post("/contract").contentType(MediaType.TEXT_PLAIN).content("루이"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unexpectedErrorHidesInternalDetails() throws Exception {
        mvc.perform(get("/contract/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().json("{\"code\":\"INTERNAL_SERVER_ERROR\",\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"서버 내부 오류가 발생했습니다.\"}"))
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
