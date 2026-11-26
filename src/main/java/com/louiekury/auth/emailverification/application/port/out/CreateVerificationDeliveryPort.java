package com.louiekury.auth.emailverification.application.port.out;

public interface CreateVerificationDeliveryPort {
    void createVerificationDelivery(String email, String code);
}
