package com.louiekury.auth.global.web;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class, SwaggerConfig.class,
    SpringDocConfigProperties.class, SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class})
@Import({AvailabilityWebAdapter.class, SignupWebAdapter.class, EmailVerificationWebAdapter.class})
@MockitoBean(types = {GetAccountAvailabilityUseCase.class, CreateSignupUseCase.class,
    CreateEmailVerificationUseCase.class, UpdateEmailVerificationUseCase.class})
@WebMvcTest({AvailabilityController.class, SignupController.class, EmailVerificationController.class})
class OpenApiDefaultProfileTest {
    private final MockMvc mvc;

    @Autowired
    OpenApiDefaultProfileTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("기본 프로필에서는 API 문서 엔드포인트를 노출하지 않는다")
    void documentationEndpointsAreNotExposedByDefault() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }
}
