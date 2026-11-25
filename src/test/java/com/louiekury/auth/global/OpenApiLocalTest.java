package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
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
    void swaggerUiIsAvailable() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
