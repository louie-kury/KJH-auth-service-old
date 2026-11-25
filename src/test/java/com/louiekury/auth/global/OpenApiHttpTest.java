package com.louiekury.auth.global;

import com.louiekury.auth.account.adapter.in.web.AvailabilityController;
import com.louiekury.auth.account.adapter.in.web.SignupController;
import com.louiekury.auth.account.application.availability.AvailabilityUseCase;
import com.louiekury.auth.account.application.signup.SignupUseCase;
import com.louiekury.auth.emailverification.adapter.in.web.EmailVerificationController;
import com.louiekury.auth.emailverification.application.confirm.ConfirmVerificationUseCase;
import com.louiekury.auth.emailverification.application.send.SendVerificationUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = OpenApiHttpTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.config.import=", "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"})
@ActiveProfiles("local")
class OpenApiHttpTest {
    @LocalServerPort int port;

    @Test
    void servesDocumentationOverHttpWithoutExternalServices() throws Exception {
        var client = HttpClient.newHttpClient();
        var docs = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, docs.statusCode());
        assertTrue(docs.headers().firstValue("content-type").orElse("").contains("application/json"));
        assertTrue(docs.body().contains("/api/auth/signup"));
        var ui = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui/index.html")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ui.statusCode());
        assertTrue(ui.body().contains("Swagger UI"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({AvailabilityController.class, SignupController.class, EmailVerificationController.class})
    static class Config {
        @Bean AvailabilityUseCase availability() { return mock(AvailabilityUseCase.class); }
        @Bean SignupUseCase signup() { return mock(SignupUseCase.class); }
        @Bean SendVerificationUseCase send() { return mock(SendVerificationUseCase.class); }
        @Bean ConfirmVerificationUseCase confirm() { return mock(ConfirmVerificationUseCase.class); }
    }
}
