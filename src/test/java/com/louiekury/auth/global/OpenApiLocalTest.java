package com.louiekury.auth.global;

import com.jayway.jsonpath.JsonPath;
import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SwaggerConfig.class, SpringDocConfigProperties.class, SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class})
@WebMvcTest({AvailabilityController.class, SignupController.class, EmailVerificationController.class})
@ActiveProfiles("local")
class OpenApiLocalTest {
    @Autowired MockMvc mvc;
    @MockitoBean AvailabilityUseCase availability;
    @MockitoBean SignupUseCase signup;
    @MockitoBean SendVerificationUseCase send;
    @MockitoBean ConfirmVerificationUseCase confirm;

    @Test
    void documentsFiveOperationsAndTheirSuccessStatuses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications'].post.responses['202']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/login-ids/availability'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/nicknames/availability'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.parameters[?(@.name == 'Idempotency-Key' && @.in == 'header')]").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/auth/login-ids/availability'].get.parameters[?(@.name == 'loginId' && @.in == 'query')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/nicknames/availability'].get.parameters[?(@.name == 'nickname' && @.in == 'query')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post.parameters[?(@.name == 'verificationId' && @.in == 'path')].required", hasItem(true)))
            .andExpect(jsonPath("$.paths['/api/auth/signup'].post.requestBody.required").value(true))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications'].post.requestBody.required").value(true))
            .andExpect(jsonPath("$.paths['/api/auth/email-verifications/{verificationId}/confirm'].post.requestBody.required").value(true));
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
    void swaggerUiIsAvailable() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
