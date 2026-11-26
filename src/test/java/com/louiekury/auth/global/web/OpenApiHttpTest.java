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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = OpenApiHttpTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.config.import=", "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigu" +
        "re.DataSourceAutoConfiguration,org.springframework.boot.hibernate.auto" +
        "configure.HibernateJpaAutoConfiguration"})
@ActiveProfiles("local")
class OpenApiHttpTest {


    @Test
    @DisplayName("외부 서비스 없이 HTTP로 API 문서를 제공한다")
    void servesDocumentationOverHttpWithoutExternalServices(@LocalServerPort int port) throws Exception {
        var client = HttpClient.newHttpClient();
        var docs = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port +
            "/v3/api-docs")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, docs.statusCode());
        assertTrue(docs.headers().firstValue("content-type").orElse("").contains("application/json"));
        assertTrue(docs.body().contains("/api/auth/signup"));
        var ui = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port +
            "/swagger-ui/index.html")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ui.statusCode());
        assertTrue(ui.body().contains("Swagger UI"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({AvailabilityController.class, SignupController.class, EmailVerificationController.class,
        AvailabilityWebAdapter.class, SignupWebAdapter.class, EmailVerificationWebAdapter.class})
    static class Config {
        @Bean GetAccountAvailabilityUseCase availability() { return mock(GetAccountAvailabilityUseCase.class); }
        @Bean CreateSignupUseCase signup() { return mock(CreateSignupUseCase.class); }
        @Bean CreateEmailVerificationUseCase send() { return mock(CreateEmailVerificationUseCase.class); }
        @Bean UpdateEmailVerificationUseCase confirm() { return mock(UpdateEmailVerificationUseCase.class); }
    }
}
