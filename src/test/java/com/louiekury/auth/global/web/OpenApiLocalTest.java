package com.louiekury.auth.global.web;

import com.jayway.jsonpath.JsonPath;
import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.adapter.in.web.AvailabilityWebAdapter;
import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.adapter.in.web.SignupWebAdapter;
import com.louiekury.auth.account.application.createsignup.CreateSignupUseCase;
import com.louiekury.auth.account.application.getaccountavailability.GetAccountAvailabilityUseCase;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationWebAdapter;
import com.louiekury.auth.emailverification.application.createemailverification.CreateEmailVerificationUseCase;
import com.louiekury.auth.emailverification.application.updateemailverification.UpdateEmailVerificationUseCase;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SwaggerConfig.class,
    SpringDocConfigProperties.class, SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class})
@Import({AvailabilityWebAdapter.class, SignupWebAdapter.class, EmailVerificationWebAdapter.class})
@MockitoBean(types = {GetAccountAvailabilityUseCase.class, CreateSignupUseCase.class,
    CreateEmailVerificationUseCase.class, UpdateEmailVerificationUseCase.class})
@WebMvcTest({AvailabilityController.class, SignupController.class, EmailVerificationController.class})
@ActiveProfiles("local")
class OpenApiLocalTest {
    private final MockMvc mvc;

    @Autowired
    OpenApiLocalTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("다섯 API 작업과 성공 상태를 문서화한다")
    void documentsFiveOperationsAndTheirSuccessStatuses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications'].post.responses['202']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post" +
                ".responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/login-ids/availability'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/nicknames/availability'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.parameters[?(@.name == 'Idempotency-K" +
                "ey' && @.in == 'header')]").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/auth/login-ids/availability'].get.parameters[?(@.name ==" +
                " 'loginId' && @.in == 'query')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/nicknames/availability'].get.parameters[?(@.name ==" +
                " 'nickname' && @.in == 'query')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post" +
                ".parameters[?(@.name == 'verificationId' && @.in == 'path')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.requestBody.required").value(true))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications'].post.requestBody.required").value(true))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post" +
                ".requestBody.required").value(true));
    }

    @Test
    @DisplayName("이메일 발송과 가입 요청은 각각의 필드를 문서화한다")
    void documentsDistinctRequestSchemas() throws Exception {
        Map<String, Object> document = apiDocument();
        assertEquals(Set.of("email", "purpose"), properties(document, schemaAt(document,
            "$.paths['/api/auth/email-verifications'].post.requestBody.content['application/json'].schema")));
        assertEquals(Set.of("name", "phoneNumber", "gender", "birthDate", "address", "nickname",
            "loginId", "password", "email", "emailVerificationToken"), properties(document, schemaAt(document,
            "$.paths['/api/auth/signup'].post.requestBody.content['application/json'].schema")));
    }

    @Test
    @DisplayName("이메일 발송과 가입 성공 응답은 각각의 데이터 필드를 문서화한다")
    void documentsDistinctSuccessResponseSchemas() throws Exception {
        Map<String, Object> document = apiDocument();
        assertEquals(Set.of("verificationId", "expiresAt"), responseDataProperties(document,
            "$.paths['/api/auth/email-verifications'].post.responses['202'].content['*/*'].schema"));
        assertEquals(Set.of("memberId"), responseDataProperties(document,
            "$.paths['/api/auth/signup'].post.responses['201'].content['*/*'].schema"));
    }

    @Test
    @DisplayName("가입 오류 응답의 문제 스키마를 문서화한다")
    void documentsProblemSchemaForSignupErrors() throws Exception {
        Map<String, Object> document = apiDocument();
        for (String status : new String[]{"400", "409"}) {
            Map<String, Object> content = JsonPath.read(document,
                "$.paths['/api/auth/signup'].post.responses['" + status + "'].content");
            assertTrue(!content.isEmpty());
            for (Object media : content.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) media;
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) entry.get("schema");
                assertEquals(Set.of("title", "status", "detail", "code"), properties(document, schema));
            }
        }
    }

    @Test
    @DisplayName("인증 확인 요청과 응답의 스키마 이름을 유지한다")
    void confirmationSchemaNamesRemainStable() throws Exception {
        Map<String, Object> document = apiDocument();
        Map<String, Object> schemas = JsonPath.read(document, "$.components.schemas");
        assertTrue(schemas.containsKey("ConfirmRequest"));
        assertTrue(schemas.containsKey("ConfirmResponse"));
    }
    @Test
    @DisplayName("다섯 인증 작업의 요약과 오류 응답 설명 및 스키마를 유지한다")
    void allOperationsKeepSummariesAndErrorSchemas() throws Exception {
        Map<String, Object> document = apiDocument();
        String[][] operations = {
            {"/api/auth/signup", "post", "일반 회원가입", "한 번만", "201"},
            {"/api/auth/login-ids/availability", "get", "아이디 중복 확인", "100 코드 포인트", "200"},
            {"/api/auth/nicknames/availability", "get", "닉네임 중복 확인", "30 코드 포인트", "200"},
            {"/api/auth/email-verifications", "post", "이메일 인증번호 발송", "SIGNUP", "202"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "이메일 인증번호 확인", "6자리", "200"}
        };
        for (String[] operation : operations) {
            String path = "$.paths['" + operation[0] + "']." + operation[1];
            assertEquals(operation[2], JsonPath.read(document, path + ".summary"), operation[0]);
            String description = JsonPath.read(document, path + ".description");
            assertTrue(description.contains(operation[3]), operation[0] + " 핵심 설명이 누락됐습니다.");
            String success = JsonPath.read(document, path + ".responses['" + operation[4] + "'].description");
            assertTrue(success.matches("(?s).*[가-힣].*"), operation[0] + " 성공 설명은 한국어여야 합니다.");
        }

        String[][] errors = {
            {"/api/auth/signup", "post", "400", "VALIDATION_ERROR 또는 INVALID_EMAIL_VERIFICATION"},
            {"/api/auth/signup", "post", "409",
                "EMAIL_ALREADY_EXISTS, LOGIN_ID_ALREADY_EXISTS, NICKNAME_ALREADY_EXISTS"},
            {"/api/auth/signup", "post", "503", "VERIFICATION_UNAVAILABLE 또는 SIGNUP_PENDING"},
            {"/api/auth/login-ids/availability", "get", "400", "VALIDATION_ERROR"},
            {"/api/auth/nicknames/availability", "get", "400", "VALIDATION_ERROR"},
            {"/api/auth/email-verifications", "post", "400", "VALIDATION_ERROR"},
            {"/api/auth/email-verifications", "post", "409", "EMAIL_ALREADY_EXISTS"},
            {"/api/auth/email-verifications", "post", "429", "RATE_LIMITED"},
            {"/api/auth/email-verifications", "post", "503",
                "VERIFICATION_DELIVERY_FAILED"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "400",
                "VALIDATION_ERROR 또는 INVALID_VERIFICATION_CODE"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "404", "NOT_FOUND"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "409", "VERIFICATION_EXPIRED"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "429", "RATE_LIMITED"},
            {"/api/auth/email-verifications/{verificationId}/confirm", "post", "503", "VERIFICATION_UNAVAILABLE"}
        };
        for (String[] error : errors) {
            String path = "$.paths['" + error[0] + "']." + error[1] + ".responses['" + error[2] + "']";
            String description = JsonPath.read(document, path + ".description");
            assertTrue(description.matches("(?s).*[가-힣].*"), path + " 오류 설명은 한국어여야 합니다.");
            for (String code : error[3].split(", | 또는 ")) {
                assertTrue(description.contains(code), path + " 오류 코드 " + code + "가 누락됐습니다.");
            }
            Map<String, Object> content = JsonPath.read(document, path + ".content");
            assertTrue(!content.isEmpty(), path);
            for (Object media : content.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) media;
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) entry.get("schema");
                assertEquals(Set.of("title", "status", "detail", "code"), properties(document, schema), path);
            }
        }
    }

    @Test
    @DisplayName("인증 확인과 중복 확인의 요청 및 성공 응답 필드를 유지한다")
    void confirmationAndAvailabilityKeepTheirSchemas() throws Exception {
        Map<String, Object> document = apiDocument();
        String confirm = "$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post";
        assertEquals(Set.of("code"), properties(document,
            schemaAt(document, confirm + ".requestBody.content['application/json'].schema")));
        assertEquals(Set.of("emailVerificationToken", "expiresAt"),
            responseDataProperties(document, confirm + ".responses['200'].content['*/*'].schema"));
        for (String path : new String[]{"/api/auth/login-ids/availability", "/api/auth/nicknames/availability"}) {
            assertEquals(Set.of("available"), responseDataProperties(document,
                "$.paths['" + path + "'].get.responses['200'].content['*/*'].schema"));
        }
    }
    private Map<String, Object> apiDocument() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.parse(json).read("$");
    }

    private Map<String, Object> schemaAt(Map<String, Object> document, String path) {
        return resolve(document, JsonPath.read(document, path));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolve(Map<String, Object> document, Map<String, Object> schema) {
        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");
            assertTrue(ref.startsWith("#/components/schemas/"), "Unexpected schema reference: " + ref);
            Map<String, Object> components = (Map<String, Object>) document.get("components");
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
            return (Map<String, Object>) schemas.get(ref.substring("#/components/schemas/".length()));
        }
        return schema;
    }

    @SuppressWarnings("unchecked")
    private Set<String> properties(Map<String, Object> document, Map<String, Object> schema) {
        return ((Map<String, Object>) resolve(document, schema).get("properties")).keySet();
    }

    @SuppressWarnings("unchecked")
    private Set<String> responseDataProperties(Map<String, Object> document, String path) {
        Map<String, Object> envelope = schemaAt(document, path);
        Map<String, Object> properties = (Map<String, Object>) envelope.get("properties");
        return properties(document, (Map<String, Object>) properties.get("data"));
    }
    @Test
    @DisplayName("로컬 프로필에서 Swagger UI를 제공한다")
    void swaggerUiIsAvailable() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
