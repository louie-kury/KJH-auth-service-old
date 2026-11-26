package com.louiekury.auth.account.application.port.out;

public interface GetRegisteredEmailPort {
    boolean getRegisteredEmailExists(String email);
}
