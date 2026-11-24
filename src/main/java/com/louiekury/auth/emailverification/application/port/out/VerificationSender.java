package com.louiekury.auth.emailverification.application.port.out;

public interface VerificationSender {
    void send(String email, String code);
}
