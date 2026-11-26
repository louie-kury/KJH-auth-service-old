package com.louiekury.auth.emailverification.application.port.out;

public interface GetRegisteredEmailPort {
    boolean getRegisteredEmailExists(String email);
}
