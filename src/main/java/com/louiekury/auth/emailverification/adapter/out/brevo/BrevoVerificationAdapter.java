package com.louiekury.auth.emailverification.adapter.out.brevo;

import com.louiekury.auth.emailverification.application.port.out.CreateVerificationDeliveryPort;
import com.louiekury.auth.shared.exception.BusinessException;
import com.louiekury.auth.shared.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BrevoVerificationAdapter implements CreateVerificationDeliveryPort {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String apiKey;
    private final String senderEmail;

    public BrevoVerificationAdapter(@Value("${verification.brevo-api-key:}") String apiKey,
                                   @Value("${verification.sender-email:}") String senderEmail) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
    }

    public void createVerificationDelivery(String email, String code) {
        if (apiKey.isBlank() || !senderEmail.matches("[A-Za-z0-9._%+@-]+") || !code.matches("[0-9]{6}")) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
        String body = "{\"sender\":{\"name\":\"Louie Kury\",\"email\":\"" + senderEmail
            + "\"},\"to\":[{\"email\":\"" + email + "\"}],\"subject\":\"회원가입 인증번호\","
            + "\"textContent\":\"인증번호: " + code + "\\n인증번호는 5분간 유효합니다.\"}";
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey).header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status < 200 || status >= 300) {
                throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.VERIFICATION_DELIVERY_FAILED);
        }
    }
}
